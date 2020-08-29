/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.IPacketReceiver;
import dan200.computercraft.api.network.IPacketSender;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class ModemPeripheral implements IPeripheral, IPacketSender, IPacketReceiver {
    private final Set<IComputerAccess> m_computers = new HashSet<>(1);
    private final ModemState m_state;
    private IPacketNetwork m_network;

    protected ModemPeripheral(ModemState state) {
        this.m_state = state;
    }

    public ModemState getModemState() {
        return this.m_state;
    }

    protected void switchNetwork() {
        this.setNetwork(this.getNetwork());
    }

    protected abstract IPacketNetwork getNetwork();

    private synchronized void setNetwork(IPacketNetwork network) {
        if (this.m_network == network) {
            return;
        }

        // Leave old network
        if (this.m_network != null) {
            this.m_network.removeReceiver(this);
        }

        // Set new network
        this.m_network = network;

        // Join new network
        if (this.m_network != null) {
            this.m_network.addReceiver(this);
        }
    }

    public void destroy() {
        this.setNetwork(null);
    }

    @Override
    public void receiveSameDimension(@Nonnull Packet packet, double distance) {
        if (packet.getSender() == this || !this.m_state.isOpen(packet.getChannel())) {
            return;
        }

        synchronized (this.m_computers) {
            for (IComputerAccess computer : this.m_computers) {
                computer.queueEvent("modem_message", new Object[] {
                    computer.getAttachmentName(),
                    packet.getChannel(),
                    packet.getReplyChannel(),
                    packet.getPayload(),
                    distance
                });
            }
        }
    }

    @Override
    public void receiveDifferentDimension(@Nonnull Packet packet) {
        if (packet.getSender() == this || !this.m_state.isOpen(packet.getChannel())) {
            return;
        }

        synchronized (this.m_computers) {
            for (IComputerAccess computer : this.m_computers) {
                computer.queueEvent("modem_message", new Object[] {
                    computer.getAttachmentName(),
                    packet.getChannel(),
                    packet.getReplyChannel(),
                    packet.getPayload()
                });
            }
        }
    }

    // IPeripheral implementation

    @Nonnull
    @Override
    public String getType() {
        return "modem";
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[] {
            "open",
            "isOpen",
            "close",
            "closeAll",
            "transmit",
            "isWireless",
            };
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
        switch (method) {
        case 0: {
            // open
            int channel = parseChannel(arguments, 0);
            this.m_state.open(channel);
            return null;
        }
        case 1: {
            // isOpen
            int channel = parseChannel(arguments, 0);
            return new Object[] {this.m_state.isOpen(channel)};
        }
        case 2: {
            // close
            int channel = parseChannel(arguments, 0);
            this.m_state.close(channel);
            return null;
        }
        case 3: // closeAll
            this.m_state.closeAll();
            return null;
        case 4: {
            // transmit
            int channel = parseChannel(arguments, 0);
            int replyChannel = parseChannel(arguments, 1);
            Object payload = arguments.length > 2 ? arguments[2] : null;
            World world = this.getWorld();
            Vec3d position = this.getPosition();
            IPacketNetwork network = this.m_network;
            if (world != null && position != null && network != null) {
                Packet packet = new Packet(channel, replyChannel, payload, this);
                if (this.isInterdimensional()) {
                    network.transmitInterdimensional(packet);
                } else {
                    network.transmitSameDimension(packet, this.getRange());
                }
            }
            return null;
        }
        case 5: {
            // isWireless
            IPacketNetwork network = this.m_network;
            return new Object[] {network != null && network.isWireless()};
        }
        default:
            return null;
        }
    }

    private static int parseChannel(Object[] arguments, int index) throws LuaException {
        int channel = getInt(arguments, index);
        if (channel < 0 || channel > 65535) {
            throw new LuaException("Expected number in range 0-65535");
        }
        return channel;
    }

    @Override
    public synchronized void attach(@Nonnull IComputerAccess computer) {
        synchronized (this.m_computers) {
            this.m_computers.add(computer);
        }

        this.setNetwork(this.getNetwork());
    }

    @Override
    public synchronized void detach(@Nonnull IComputerAccess computer) {
        boolean empty;
        synchronized (this.m_computers) {
            this.m_computers.remove(computer);
            empty = this.m_computers.isEmpty();
        }

        if (empty) {
            this.setNetwork(null);
        }
    }

    @Nonnull
    @Override
    public String getSenderID() {
        synchronized (this.m_computers) {
            if (this.m_computers.size() != 1) {
                return "unknown";
            } else {
                IComputerAccess computer = this.m_computers.iterator()
                                                           .next();
                return computer.getID() + "_" + computer.getAttachmentName();
            }
        }
    }
}
