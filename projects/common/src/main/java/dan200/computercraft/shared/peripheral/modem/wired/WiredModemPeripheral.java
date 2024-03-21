// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.network.wired.WiredSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.NotAttachedException;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.computer.GuardedLuaContext;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.core.util.LuaUtil;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class WiredModemPeripheral extends ModemPeripheral implements WiredSender {
    private static final Logger LOG = LoggerFactory.getLogger(WiredModemPeripheral.class);

    private final WiredModemElement modem;
    private final WiredModemLocalPeripheral localPeripheral;
    private final BlockEntity target;

    private final Map<IComputerAccess, ConcurrentMap<String, RemotePeripheralWrapper>> peripheralWrappers = new HashMap<>(1);

    public WiredModemPeripheral(
        ModemState state,
        WiredModemElement modem,
        WiredModemLocalPeripheral localPeripheral,
        BlockEntity target
    ) {
        super(state);
        this.modem = modem;
        this.localPeripheral = localPeripheral;
        this.target = target;
    }

    //region IPacketSender implementation
    @Override
    public boolean isInterdimensional() {
        return true;
    }

    @Override
    public double getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected PacketNetwork getNetwork() {
        return modem.getNode();
    }

    @Override
    public Level getLevel() {
        return modem.getLevel();
    }
    //endregion

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("peripheral_hub");
    }

    //region Peripheral methods

    /**
     * List all remote peripherals on the wired network.
     * <p>
     * If this computer is attached to the network, it _will not_ be included in
     * this list.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer The calling computer.
     * @return Remote peripheral names on the network.
     */
    @LuaFunction
    public final Collection<String> getNamesRemote(IComputerAccess computer) {
        var wrappers = getWrappers(computer);
        return wrappers == null ? Set.of() : wrappers.keySet();
    }

    /**
     * Determine if a peripheral is available on this wired network.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return boolean If a peripheral is present with the given name.
     * @see PeripheralAPI#isPresent
     */
    @LuaFunction
    public final boolean isPresentRemote(IComputerAccess computer, String name) {
        return getWrapper(computer, name) != null;
    }

    /**
     * Get the type of a peripheral is available on this wired network.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return The peripheral's name.
     * @cc.treturn string|nil The peripheral's type, or {@code nil} if it is not present.
     * @cc.changed 1.99 Peripherals can have multiple types - this function returns multiple values.
     * @see PeripheralAPI#getType
     */
    @LuaFunction
    public final @Nullable Object[] getTypeRemote(IComputerAccess computer, String name) {
        var wrapper = getWrapper(computer, name);
        return wrapper == null ? null : LuaUtil.consArray(wrapper.getType(), wrapper.getAdditionalTypes());
    }

    /**
     * Check a peripheral is of a particular type.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @param type     The type to check.
     * @return The peripheral's name.
     * @cc.treturn boolean|nil If a peripheral has a particular type, or {@literal nil} if it is not present.
     * @cc.since 1.99
     * @see PeripheralAPI#getType
     */
    @LuaFunction
    public final @Nullable Object[] hasTypeRemote(IComputerAccess computer, String name, String type) {
        var wrapper = getWrapper(computer, name);
        return wrapper == null ? null : new Object[]{ wrapper.getType().equals(type) || wrapper.getAdditionalTypes().contains(type) };
    }

    /**
     * Get all available methods for the remote peripheral with the given name.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer The calling computer.
     * @param name     The peripheral's name.
     * @return A list of methods provided by this peripheral, or {@code nil} if it is not present.
     * @cc.treturn { string... }|nil A list of methods provided by this peripheral, or {@code nil} if it is not present.
     * @see PeripheralAPI#getMethods
     */
    @LuaFunction
    public final @Nullable Object[] getMethodsRemote(IComputerAccess computer, String name) {
        var wrapper = getWrapper(computer, name);
        if (wrapper == null) return null;

        return new Object[]{ wrapper.getMethodNames() };
    }

    /**
     * Call a method on a peripheral on this wired network.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @param computer  The calling computer.
     * @param context   The Lua context we're executing in.
     * @param arguments Arguments to this computer.
     * @return The peripheral's result.
     * @throws LuaException (hidden) If the method throws an error.
     * @cc.tparam string remoteName The name of the peripheral to invoke the method on.
     * @cc.tparam string method The name of the method
     * @cc.param ...      Additional arguments to pass to the method
     * @cc.treturn string The return values of the peripheral method.
     * @see PeripheralAPI#call
     */
    @LuaFunction
    public final MethodResult callRemote(IComputerAccess computer, ILuaContext context, IArguments arguments) throws LuaException {
        var remoteName = arguments.getString(0);
        var methodName = arguments.getString(1);
        var wrapper = getWrapper(computer, remoteName);
        if (wrapper == null) throw new LuaException("No peripheral: " + remoteName);

        return wrapper.callMethod(context, methodName, arguments.drop(2));
    }

    /**
     * Returns the network name of the current computer, if the modem is on. This
     * may be used by other computers on the network to wrap this computer as a
     * peripheral.
     * <p>
     * > [!NOTE]
     * > This function only appears on wired modems. Check {@link #isWireless} returns false before calling it.
     *
     * @return The current computer's name.
     * @cc.treturn string|nil The current computer's name on the wired network.
     * @cc.since 1.80pr1.7
     */
    @LuaFunction
    public final @Nullable Object[] getNameLocal() {
        var local = localPeripheral.getConnectedName();
        return local == null ? null : new Object[]{ local };
    }

    @Override
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void attach(IComputerAccess computer) {
        super.attach(computer);

        ConcurrentMap<String, RemotePeripheralWrapper> wrappers;
        synchronized (peripheralWrappers) {
            wrappers = peripheralWrappers.computeIfAbsent(computer, k -> new ConcurrentHashMap<>());
        }

        synchronized (modem.getRemotePeripherals()) {
            for (var entry : modem.getRemotePeripherals().entrySet()) {
                attachPeripheralImpl(computer, wrappers, entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    @SuppressWarnings("UnsynchronizedOverridesSynchronized")
    public void detach(IComputerAccess computer) {
        Map<String, RemotePeripheralWrapper> wrappers;
        synchronized (peripheralWrappers) {
            wrappers = peripheralWrappers.remove(computer);
        }
        if (wrappers != null) {
            for (var wrapper : wrappers.values()) wrapper.detach();
            wrappers.clear();
        }

        super.detach(computer);
    }

    @Override
    public final boolean equals(@Nullable IPeripheral other) {
        return other instanceof WiredModemPeripheral otherModem && otherModem.modem == modem;
    }

    @Override
    public final Object getTarget() {
        return target;
    }
    //endregion

    @Override
    public WiredNode getNode() {
        return modem.getNode();
    }

    public void attachPeripheral(String name, IPeripheral peripheral) {
        synchronized (peripheralWrappers) {
            for (var entry : peripheralWrappers.entrySet()) {
                attachPeripheralImpl(entry.getKey(), entry.getValue(), name, peripheral);
            }
        }
    }

    public void detachPeripheral(String name) {
        synchronized (peripheralWrappers) {
            for (var wrappers : peripheralWrappers.values()) {
                var wrapper = wrappers.remove(name);
                if (wrapper != null) wrapper.detach();
            }
        }
    }

    private void attachPeripheralImpl(IComputerAccess computer, ConcurrentMap<String, RemotePeripheralWrapper> peripherals, String periphName, IPeripheral peripheral) {
        if (!peripherals.containsKey(periphName) && !periphName.equals(localPeripheral.getConnectedName())) {
            var methods = ServerContext.get(((ServerLevel) getLevel()).getServer()).peripheralMethods().getSelfMethods(peripheral);
            var wrapper = new RemotePeripheralWrapper(modem, peripheral, computer, periphName, methods);
            peripherals.put(periphName, wrapper);
            wrapper.attach();
        }
    }

    private @Nullable ConcurrentMap<String, RemotePeripheralWrapper> getWrappers(IComputerAccess computer) {
        synchronized (peripheralWrappers) {
            return peripheralWrappers.get(computer);
        }
    }

    private @Nullable RemotePeripheralWrapper getWrapper(IComputerAccess computer, String remoteName) {
        var wrappers = getWrappers(computer);
        return wrappers == null ? null : wrappers.get(remoteName);
    }

    private static class RemotePeripheralWrapper implements IComputerAccess, GuardedLuaContext.Guard {
        private final WiredModemElement element;
        private final IPeripheral peripheral;
        private final IComputerAccess computer;
        private final String name;

        private final String type;
        private final Set<String> additionalTypes;
        private final Map<String, PeripheralMethod> methodMap;

        private volatile boolean attached;
        private final Set<String> mounts = new HashSet<>();

        private @Nullable GuardedLuaContext contextWrapper;

        RemotePeripheralWrapper(WiredModemElement element, IPeripheral peripheral, IComputerAccess computer, String name, Map<String, PeripheralMethod> methods) {
            this.element = element;
            this.peripheral = peripheral;
            this.computer = computer;
            this.name = name;

            type = Objects.requireNonNull(peripheral.getType(), "Peripheral type cannot be null");
            additionalTypes = peripheral.getAdditionalTypes();
            methodMap = methods;
        }

        public void attach() {
            attached = true;
            peripheral.attach(this);
            computer.queueEvent("peripheral", getAttachmentName());
        }

        public void detach() {
            peripheral.detach(this);
            computer.queueEvent("peripheral_detach", getAttachmentName());
            attached = false;

            synchronized (this) {
                if (!mounts.isEmpty()) {
                    LOG.warn("Peripheral {} called mount but did not call unmount for {}", peripheral, mounts);
                }

                for (var mount : mounts) computer.unmount(mount);
                mounts.clear();
            }
        }

        public String getType() {
            return type;
        }

        public Set<String> getAdditionalTypes() {
            return additionalTypes;
        }

        public Collection<String> getMethodNames() {
            return methodMap.keySet();
        }

        public MethodResult callMethod(ILuaContext context, String methodName, IArguments arguments) throws LuaException {
            var method = methodMap.get(methodName);
            if (method == null) throw new LuaException("No such method " + methodName);

            // Wrap the ILuaContext. We try to reuse the previous context where possible to avoid allocations.
            var contextWrapper = this.contextWrapper;
            if (contextWrapper == null || !contextWrapper.wraps(context)) {
                contextWrapper = this.contextWrapper = new GuardedLuaContext(context, this);
            }

            return method.apply(peripheral, contextWrapper, this, arguments);
        }

        @Override
        public boolean checkValid() {
            return attached;
        }

        // IComputerAccess implementation

        @Override
        public synchronized @Nullable String mount(String desiredLocation, Mount mount) {
            if (!attached) throw new NotAttachedException();
            var mounted = computer.mount(desiredLocation, mount, name);
            mounts.add(mounted);
            return mounted;
        }

        @Override
        public synchronized @Nullable String mount(String desiredLocation, Mount mount, String driveName) {
            if (!attached) throw new NotAttachedException();
            var mounted = computer.mount(desiredLocation, mount, driveName);
            mounts.add(mounted);
            return mounted;
        }

        @Override
        public synchronized @Nullable String mountWritable(String desiredLocation, WritableMount mount) {
            if (!attached) throw new NotAttachedException();
            var mounted = computer.mountWritable(desiredLocation, mount, name);
            mounts.add(mounted);
            return mounted;
        }

        @Override
        public synchronized @Nullable String mountWritable(String desiredLocation, WritableMount mount, String driveName) {
            if (!attached) throw new NotAttachedException();
            var mounted = computer.mountWritable(desiredLocation, mount, driveName);
            mounts.add(mounted);
            return mounted;
        }

        @Override
        public synchronized void unmount(@Nullable String location) {
            if (!attached) throw new NotAttachedException();
            computer.unmount(location);
            mounts.remove(location);
        }

        @Override
        public int getID() {
            if (!attached) throw new NotAttachedException();
            return computer.getID();
        }

        @Override
        public void queueEvent(String event, @Nullable Object... arguments) {
            if (!attached) throw new NotAttachedException();
            computer.queueEvent(event, arguments);
        }

        @Override
        public WorkMonitor getMainThreadMonitor() {
            if (!attached) throw new NotAttachedException();
            return computer.getMainThreadMonitor();
        }

        @Override
        public String getAttachmentName() {
            if (!attached) throw new NotAttachedException();
            return name;
        }

        @Override
        public Map<String, IPeripheral> getAvailablePeripherals() {
            if (!attached) throw new NotAttachedException();
            synchronized (element.getRemotePeripherals()) {
                return Map.copyOf(element.getRemotePeripherals());
            }
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral(String name) {
            if (!attached) throw new NotAttachedException();
            synchronized (element.getRemotePeripherals()) {
                return element.getRemotePeripherals().get(name);
            }
        }
    }
}
