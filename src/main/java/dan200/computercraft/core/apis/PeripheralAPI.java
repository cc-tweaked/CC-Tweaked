/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.tracking.TrackingField;

public class PeripheralAPI implements ILuaAPI, IAPIEnvironment.IPeripheralChangeListener {
    private final IAPIEnvironment m_environment;
    private final PeripheralWrapper[] m_peripherals;
    private boolean m_running;
    public PeripheralAPI(IAPIEnvironment environment) {
        this.m_environment = environment;
        this.m_environment.setPeripheralChangeListener(this);

        this.m_peripherals = new PeripheralWrapper[6];
        for (int i = 0; i < 6; i++) {
            this.m_peripherals[i] = null;
        }

        this.m_running = false;
    }

    @Override
    public void onPeripheralChanged(ComputerSide side, IPeripheral newPeripheral) {
        synchronized (this.m_peripherals) {
            int index = side.ordinal();
            if (this.m_peripherals[index] != null) {
                // Queue a detachment
                final PeripheralWrapper wrapper = this.m_peripherals[index];
                if (wrapper.isAttached()) {
                    wrapper.detach();
                }

                // Queue a detachment event
                this.m_environment.queueEvent("peripheral_detach", new Object[] {side.getName()});
            }

            // Assign the new peripheral
            this.m_peripherals[index] = newPeripheral == null ? null : new PeripheralWrapper(newPeripheral, side.getName());

            if (this.m_peripherals[index] != null) {
                // Queue an attachment
                final PeripheralWrapper wrapper = this.m_peripherals[index];
                if (this.m_running && !wrapper.isAttached()) {
                    wrapper.attach();
                }

                // Queue an attachment event
                this.m_environment.queueEvent("peripheral", new Object[] {side.getName()});
            }
        }
    }

    // IPeripheralChangeListener

    @Override
    public String[] getNames() {
        return new String[] {
            "peripheral"
        };
    }

    // ILuaAPI implementation

    @Override
    public void startup() {
        synchronized (this.m_peripherals) {
            this.m_running = true;
            for (int i = 0; i < 6; i++) {
                PeripheralWrapper wrapper = this.m_peripherals[i];
                if (wrapper != null && !wrapper.isAttached()) {
                    wrapper.attach();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (this.m_peripherals) {
            this.m_running = false;
            for (int i = 0; i < 6; i++) {
                PeripheralWrapper wrapper = this.m_peripherals[i];
                if (wrapper != null && wrapper.isAttached()) {
                    wrapper.detach();
                }
            }
        }
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[] {
            "isPresent",
            "getType",
            "getMethods",
            "call"
        };
    }

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
        switch (method) {
        case 0: {
            // isPresent
            boolean present = false;
            ComputerSide side = ComputerSide.valueOfInsensitive(getString(args, 0));
            if (side != null) {
                synchronized (this.m_peripherals) {
                    PeripheralWrapper p = this.m_peripherals[side.ordinal()];
                    if (p != null) {
                        present = true;
                    }
                }
            }
            return new Object[] {present};
        }
        case 1: {
            // getType
            ComputerSide side = ComputerSide.valueOfInsensitive(getString(args, 0));
            if (side != null) {
                String type = null;
                synchronized (this.m_peripherals) {
                    PeripheralWrapper p = this.m_peripherals[side.ordinal()];
                    if (p != null) {
                        return new Object[] {p.getType()};
                    }
                }
            }
            return null;
        }
        case 2: {
            // getMethods
            String[] methods = null;
            ComputerSide side = ComputerSide.valueOfInsensitive(getString(args, 0));
            if (side != null) {
                synchronized (this.m_peripherals) {
                    PeripheralWrapper p = this.m_peripherals[side.ordinal()];
                    if (p != null) {
                        methods = p.getMethods();
                    }
                }
            }
            if (methods != null) {
                Map<Object, Object> table = new HashMap<>();
                for (int i = 0; i < methods.length; i++) {
                    table.put(i + 1, methods[i]);
                }
                return new Object[] {table};
            }
            return null;
        }
        case 3: {
            // call
            ComputerSide side = ComputerSide.valueOfInsensitive(getString(args, 0));
            String methodName = getString(args, 1);
            Object[] methodArgs = Arrays.copyOfRange(args, 2, args.length);

            if (side != null) {
                PeripheralWrapper p;
                synchronized (this.m_peripherals) {
                    p = this.m_peripherals[side.ordinal()];
                }
                if (p != null) {
                    return p.call(context, methodName, methodArgs);
                }
            }
            throw new LuaException("No peripheral attached");
        }
        default:
            return null;
        }
    }

    private class PeripheralWrapper extends ComputerAccess {
        private final String m_side;
        private final IPeripheral m_peripheral;

        private String m_type;
        private String[] m_methods;
        private Map<String, Integer> m_methodMap;
        private boolean m_attached;

        public PeripheralWrapper(IPeripheral peripheral, String side) {
            super(PeripheralAPI.this.m_environment);
            this.m_side = side;
            this.m_peripheral = peripheral;
            this.m_attached = false;

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

        public String getType() {
            return this.m_type;
        }        public IPeripheral getPeripheral() {
            return this.m_peripheral;
        }

        public String[] getMethods() {
            return this.m_methods;
        }

        public synchronized void attach() {
            this.m_attached = true;
            this.m_peripheral.attach(this);
        }

        public void detach() {
            // Call detach
            this.m_peripheral.detach(this);

            synchronized (this) {
                // Unmount everything the detach function forgot to do
                this.unmountAll();
            }

            this.m_attached = false;
        }        public synchronized boolean isAttached() {
            return this.m_attached;
        }

        public Object[] call(ILuaContext context, String methodName, Object[] arguments) throws LuaException, InterruptedException {
            int method = -1;
            synchronized (this) {
                if (this.m_methodMap.containsKey(methodName)) {
                    method = this.m_methodMap.get(methodName);
                }
            }
            if (method >= 0) {
                PeripheralAPI.this.m_environment.addTrackingChange(TrackingField.PERIPHERAL_OPS);
                return this.m_peripheral.callMethod(this, context, method, arguments);
            } else {
                throw new LuaException("No such method " + methodName);
            }
        }

        // IComputerAccess implementation
        @Override
        public synchronized String mount(@Nonnull String desiredLoc, @Nonnull IMount mount, @Nonnull String driveName) {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }

            return super.mount(desiredLoc, mount, driveName);
        }

        @Override
        public synchronized String mountWritable(@Nonnull String desiredLoc, @Nonnull IWritableMount mount, @Nonnull String driveName) {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }

            return super.mountWritable(desiredLoc, mount, driveName);
        }

        @Override
        public synchronized void unmount(String location) {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }

            super.unmount(location);
        }

        @Override
        public int getID() {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }
            return super.getID();
        }

        @Override
        public void queueEvent(@Nonnull final String event, final Object[] arguments) {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }
            super.queueEvent(event, arguments);
        }





        @Nonnull
        @Override
        public String getAttachmentName() {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }
            return this.m_side;
        }

        @Nonnull
        @Override
        public Map<String, IPeripheral> getAvailablePeripherals() {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }

            Map<String, IPeripheral> peripherals = new HashMap<>();
            for (PeripheralWrapper wrapper : PeripheralAPI.this.m_peripherals) {
                if (wrapper != null && wrapper.isAttached()) {
                    peripherals.put(wrapper.getAttachmentName(), wrapper.getPeripheral());
                }
            }

            return Collections.unmodifiableMap(peripherals);
        }

        @Nullable
        @Override
        public IPeripheral getAvailablePeripheral(@Nonnull String name) {
            if (!this.m_attached) {
                throw new RuntimeException("You are not attached to this Computer");
            }

            for (PeripheralWrapper wrapper : PeripheralAPI.this.m_peripherals) {
                if (wrapper != null && wrapper.isAttached() && wrapper.getAttachmentName()
                                                                      .equals(name)) {
                    return wrapper.getPeripheral();
                }
            }
            return null;
        }
    }
}
