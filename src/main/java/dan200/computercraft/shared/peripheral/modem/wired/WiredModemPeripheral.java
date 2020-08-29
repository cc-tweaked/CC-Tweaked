/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;

import net.minecraft.world.World;

public abstract class WiredModemPeripheral extends ModemPeripheral implements IWiredSender {
    private final WiredModemElement modem;

    private final Map<IComputerAccess, ConcurrentMap<String, RemotePeripheralWrapper>> peripheralWrappers = new HashMap<>(1);

    public WiredModemPeripheral(ModemState state, WiredModemElement modem) {
        super(state);
        this.modem = modem;
    }

    @Override
    public double getRange() {
        return 256.0;
    }

    //region IPacketSender implementation
    @Override
    public boolean isInterdimensional() {
        return false;
    }

    @Override
    protected IPacketNetwork getNetwork() {
        return this.modem.getNode();
    }

    //region IPeripheral
    @Nonnull
    @Override
    public String[] getMethodNames() {
        String[] methods = super.getMethodNames();
        String[] newMethods = new String[methods.length + 6];
        System.arraycopy(methods, 0, newMethods, 0, methods.length);
        newMethods[methods.length] = "getNamesRemote";
        newMethods[methods.length + 1] = "isPresentRemote";
        newMethods[methods.length + 2] = "getTypeRemote";
        newMethods[methods.length + 3] = "getMethodsRemote";
        newMethods[methods.length + 4] = "callRemote";
        newMethods[methods.length + 5] = "getNameLocal";
        return newMethods;
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
        String[] methods = super.getMethodNames();
        switch (method - methods.length) {
        case 0: {
            // getNamesRemote
            Map<String, RemotePeripheralWrapper> wrappers = this.getWrappers(computer);
            Map<Object, Object> table = new HashMap<>();
            if (wrappers != null) {
                int idx = 1;
                for (String name : wrappers.keySet()) {
                    table.put(idx++, name);
                }
            }
            return new Object[] {table};
        }
        case 1: {
            // isPresentRemote
            String name = getString(arguments, 0);
            return new Object[] {this.getWrapper(computer, name) != null};
        }
        case 2: {
            // getTypeRemote
            String name = getString(arguments, 0);
            RemotePeripheralWrapper wrapper = this.getWrapper(computer, name);
            return wrapper != null ? new Object[] {wrapper.getType()} : null;
        }
        case 3: {
            // getMethodsRemote
            String name = getString(arguments, 0);
            RemotePeripheralWrapper wrapper = this.getWrapper(computer, name);
            if (wrapper == null) {
                return null;
            }

            String[] methodNames = wrapper.getMethodNames();
            Map<Object, Object> table = new HashMap<>();
            for (int i = 0; i < methodNames.length; i++) {
                table.put(i + 1, methodNames[i]);
            }
            return new Object[] {table};
        }
        case 4: {
            // callRemote
            String remoteName = getString(arguments, 0);
            String methodName = getString(arguments, 1);
            RemotePeripheralWrapper wrapper = this.getWrapper(computer, remoteName);
            if (wrapper == null) {
                throw new LuaException("No peripheral: " + remoteName);
            }

            Object[] methodArgs = new Object[arguments.length - 2];
            System.arraycopy(arguments, 2, methodArgs, 0, arguments.length - 2);
            return wrapper.callMethod(context, methodName, methodArgs);
        }
        case 5: {
            // getNameLocal
            String local = this.getLocalPeripheral().getConnectedName();
            return local == null ? null : new Object[] {local};
        }
        default: {
            // The regular modem methods
            return super.callMethod(computer, context, method, arguments);
        }
        }
    }
    //endregion

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        super.attach(computer);

        ConcurrentMap<String, RemotePeripheralWrapper> wrappers;
        synchronized (this.peripheralWrappers) {
            wrappers = this.peripheralWrappers.get(computer);
            if (wrappers == null) {
                this.peripheralWrappers.put(computer, wrappers = new ConcurrentHashMap<>());
            }
        }

        synchronized (this.modem.getRemotePeripherals()) {
            for (Map.Entry<String, IPeripheral> entry : this.modem.getRemotePeripherals()
                                                                  .entrySet()) {
                this.attachPeripheralImpl(computer, wrappers, entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        Map<String, RemotePeripheralWrapper> wrappers;
        synchronized (this.peripheralWrappers) {
            wrappers = this.peripheralWrappers.remove(computer);
        }
        if (wrappers != null) {
            for (RemotePeripheralWrapper wrapper : wrappers.values()) {
                wrapper.detach();
            }
            wrappers.clear();
        }

        super.detach(computer);
    }

    private void attachPeripheralImpl(IComputerAccess computer, ConcurrentMap<String, RemotePeripheralWrapper> peripherals, String periphName,
                                      IPeripheral peripheral) {
        if (!peripherals.containsKey(periphName) && !periphName.equals(this.getLocalPeripheral().getConnectedName())) {
            RemotePeripheralWrapper wrapper = new RemotePeripheralWrapper(this.modem, peripheral, computer, periphName);
            peripherals.put(periphName, wrapper);
            wrapper.attach();
        }
    }

    private ConcurrentMap<String, RemotePeripheralWrapper> getWrappers(IComputerAccess computer) {
        synchronized (this.peripheralWrappers) {
            return this.peripheralWrappers.get(computer);
        }
    }

    private RemotePeripheralWrapper getWrapper(IComputerAccess computer, String remoteName) {
        ConcurrentMap<String, RemotePeripheralWrapper> wrappers = this.getWrappers(computer);
        return wrappers == null ? null : wrappers.get(remoteName);
    }
    //endregion

    @Nonnull
    protected abstract WiredModemLocalPeripheral getLocalPeripheral();

    @Nonnull
    @Override
    public World getWorld() {
        return this.modem.getWorld();
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (other instanceof WiredModemPeripheral) {
            WiredModemPeripheral otherModem = (WiredModemPeripheral) other;
            return otherModem.modem == this.modem;
        }
        return false;
    }

    @Nonnull
    @Override
    public IWiredNode getNode() {
        return this.modem.getNode();
    }

    public void attachPeripheral(String name, IPeripheral peripheral) {
        synchronized (this.peripheralWrappers) {
            for (Map.Entry<IComputerAccess, ConcurrentMap<String, RemotePeripheralWrapper>> entry : this.peripheralWrappers.entrySet()) {
                this.attachPeripheralImpl(entry.getKey(), entry.getValue(), name, peripheral);
            }
        }
    }

    public void detachPeripheral(String name) {
        synchronized (this.peripheralWrappers) {
            for (ConcurrentMap<String, RemotePeripheralWrapper> wrappers : this.peripheralWrappers.values()) {
                RemotePeripheralWrapper wrapper = wrappers.remove(name);
                if (wrapper != null) {
                    wrapper.detach();
                }
            }

        }
    }

    private static class RemotePeripheralWrapper implements IComputerAccess {
        private final WiredModemElement m_element;
        private final IPeripheral m_peripheral;
        private final IComputerAccess m_computer;
        private final String m_name;

        private final String m_type;
        private final String[] m_methods;
        private final Map<String, Integer> m_methodMap;

        public RemotePeripheralWrapper(WiredModemElement element, IPeripheral peripheral, IComputerAccess computer, String name) {
            this.m_element = element;
            this.m_peripheral = peripheral;
            this.m_computer = computer;
            this.m_name = name;

            this.m_type = peripheral.getType();
            this.m_methods = peripheral.getMethodNames();
            assert this.m_type != null;
            assert this.m_methods != null;

            this.m_methodMap = new HashMap<>();
            for (int i = 0; i < this.m_methods.length; i++) {
                if (this.m_methods[i] != null) {
                    this.m_methodMap.put(this.m_methods[i], i);
                }
            }
        }

        public void attach() {
            this.m_peripheral.attach(this);
            this.m_computer.queueEvent("peripheral", new Object[] {this.getAttachmentName()});
        }

        public void detach() {
            this.m_peripheral.detach(this);
            this.m_computer.queueEvent("peripheral_detach", new Object[] {this.getAttachmentName()});
        }

        public String getType() {
            return this.m_type;
        }

        public String[] getMethodNames() {
            return this.m_methods;
        }

        public Object[] callMethod(ILuaContext context, String methodName, Object[] arguments) throws LuaException, InterruptedException {
            if (this.m_methodMap.containsKey(methodName)) {
                int method = this.m_methodMap.get(methodName);
                return this.m_peripheral.callMethod(this, context, method, arguments);
            }
            throw new LuaException("No such method " + methodName);
        }

        // IComputerAccess implementation

        @Override
        public String mount(@Nonnull String desiredLocation, @Nonnull IMount mount) {
            return this.m_computer.mount(desiredLocation, mount, this.m_name);
        }

        @Override
        public String mount(@Nonnull String desiredLocation, @Nonnull IMount mount, @Nonnull String driveName) {
            return this.m_computer.mount(desiredLocation, mount, driveName);
        }

        @Nonnull
        @Override
        public String getAttachmentName() {
            return this.m_name;
        }

        @Override
        public String mountWritable(@Nonnull String desiredLocation, @Nonnull IWritableMount mount) {
            return this.m_computer.mountWritable(desiredLocation, mount, this.m_name);
        }

        @Override
        public String mountWritable(@Nonnull String desiredLocation, @Nonnull IWritableMount mount, @Nonnull String driveName) {
            return this.m_computer.mountWritable(desiredLocation, mount, driveName);
        }

        @Override
        public void unmount(String location) {
            this.m_computer.unmount(location);
        }

        @Override
        public int getID() {
            return this.m_computer.getID();
        }

        @Override
        public void queueEvent(@Nonnull String event, Object[] arguments) {
            this.m_computer.queueEvent(event, arguments);
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals() {
            synchronized (this.m_element.getRemotePeripherals()) {
                return ImmutableMap.copyOf(this.m_element.getRemotePeripherals());
            }
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral(@Nonnull String name) {
            synchronized (this.m_element.getRemotePeripherals()) {
                return this.m_element.getRemotePeripherals()
                                     .get(name);
            }
        }

        @Nullable
        @Override
        public IWorkMonitor getMainThreadMonitor() {
            return this.m_computer.getMainThreadMonitor();
        }
    }
}
