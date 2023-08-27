// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.api.network.PacketReceiver;
import dan200.computercraft.api.network.PacketSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Modems allow you to send messages between computers over long distances.
 * <p>
 * > [!TIP]
 * > Modems provide a fairly basic set of methods, which makes them very flexible but often hard to work with. The
 * > [`rednet`] API is built on top of modems, and provides a more user-friendly interface.
 * <p>
 * ## Sending and receiving messages
 * Modems operate on a series of channels, a bit like frequencies on a radio. Any modem can send a message on a
 * particular channel, but only those which have {@link #open opened} the channel and are "listening in" can receive
 * messages.
 * <p>
 * Channels are represented as an integer between 0 and 65535 inclusive. These channels don't have any defined meaning,
 * though some APIs or programs will assign a meaning to them. For instance, the [`gps`] module sends all its messages on
 * channel 65534 ([`gps.CHANNEL_GPS`]), while [`rednet`] uses channels equal to the computer's ID.
 * <p>
 * - Sending messages is done with the {@link #transmit(int, int, Object)} message.
 * - Receiving messages is done by listening to the [`modem_message`] event.
 * <p>
 * ## Types of modem
 * CC: Tweaked comes with three kinds of modem, with different capabilities.
 *
 * <ul>
 * <li><strong>Wireless modems:</strong> Wireless modems can send messages to any other wireless modem. They can be placed next to a
 * computer, or equipped as a pocket computer or turtle upgrade.
 * <p>
 * Wireless modems have a limited range, only sending messages to modems within 64 blocks. This range increases
 * linearly once the modem is above y=96, to a maximum of 384 at world height.</li>
 * <li><strong>Ender modems:</strong> These are upgraded versions of normal wireless modems. They do not have a distance
 * limit, and can send messages between dimensions.</li>
 * <li><strong>Wired modems:</strong> These send messages to other any other wired modems connected to the same network
 * (using <em>Networking Cable</em>). They also can be used to attach additional peripherals to a computer.</li></ul>
 *
 * ## Recipes
 * <div class="recipe-container">
 *     <mc-recipe recipe="computercraft:wireless_modem_normal"></mc-recipe>
 *     <mc-recipe recipe="computercraft:wireless_modem_advanced"></mc-recipe>
 *     <mc-recipe recipe="computercraft:wired_modem"></mc-recipe>
 *     <mc-recipe recipe="computercraft:cable"></mc-recipe>
 *     <mc-recipe recipe="computercraft:wired_modem_full_from"></mc-recipe>
 * </div>
 *
 * @cc.module modem
 * @cc.see modem_message Queued when a modem receives a message on an {@link #open(int) open channel}.
 * @cc.see rednet A networking API built on top of the modem peripheral.
 * @cc.usage Wrap a modem and a message on channel 15, requesting a response on channel 43. Then wait for a message to
 * arrive on channel 43 and print it.
 *
 * <pre>{@code
 * local modem = peripheral.find("modem") or error("No modem attached", 0)
 * modem.open(43) -- Open 43 so we can receive replies
 *
 * -- Send our message
 * modem.transmit(15, 43, "Hello, world!")
 *
 * -- And wait for a reply
 * local event, side, channel, replyChannel, message, distance
 * repeat
 *   event, side, channel, replyChannel, message, distance = os.pullEvent("modem_message")
 * until channel == 43
 *
 * print("Received a reply: " .. tostring(message))
 * }</pre>
 * <p>
 */
public abstract class ModemPeripheral implements IPeripheral, PacketSender, PacketReceiver {
    private @Nullable PacketNetwork network;
    private final @GuardedBy("computers") Set<IComputerAccess> computers = new HashSet<>(1);
    private final ModemState state;

    protected ModemPeripheral(ModemState state) {
        this.state = state;
    }

    public ModemState getModemState() {
        return state;
    }

    private synchronized void setNetwork(@Nullable PacketNetwork network) {
        if (this.network == network) return;

        // Leave old network
        if (this.network != null) this.network.removeReceiver(this);

        // Set new network
        this.network = network;

        // Join new network
        if (this.network != null) this.network.addReceiver(this);
    }

    public void removed() {
        setNetwork(null);
    }

    @Override
    public void receiveSameDimension(Packet packet, double distance) {
        if (packet.sender() == this || !state.isOpen(packet.channel())) return;

        synchronized (computers) {
            for (var computer : computers) {
                computer.queueEvent("modem_message",
                    computer.getAttachmentName(), packet.channel(), packet.replyChannel(), packet.payload(), distance);
            }
        }
    }

    @Override
    public void receiveDifferentDimension(Packet packet) {
        if (packet.sender() == this || !state.isOpen(packet.channel())) return;

        synchronized (computers) {
            for (var computer : computers) {
                computer.queueEvent("modem_message",
                    computer.getAttachmentName(), packet.channel(), packet.replyChannel(), packet.payload());
            }
        }
    }

    protected abstract PacketNetwork getNetwork();

    @Override
    public String getType() {
        return "modem";
    }

    private static int parseChannel(int channel) throws LuaException {
        if (channel < 0 || channel > 65535) throw new LuaException("Expected number in range 0-65535");
        return channel;
    }

    /**
     * Open a channel on a modem. A channel must be open in order to receive messages. Modems can have up to 128
     * channels open at one time.
     *
     * @param channel The channel to open. This must be a number between 0 and 65535.
     * @throws LuaException If the channel is out of range.
     * @throws LuaException If there are too many open channels.
     */
    @LuaFunction
    public final void open(int channel) throws LuaException {
        state.open(parseChannel(channel));
    }

    /**
     * Check if a channel is open.
     *
     * @param channel The channel to check.
     * @return Whether the channel is open.
     * @throws LuaException If the channel is out of range.
     */
    @LuaFunction
    public final boolean isOpen(int channel) throws LuaException {
        return state.isOpen(parseChannel(channel));
    }

    /**
     * Close an open channel, meaning it will no longer receive messages.
     *
     * @param channel The channel to close.
     * @throws LuaException If the channel is out of range.
     */
    @LuaFunction
    public final void close(int channel) throws LuaException {
        state.close(parseChannel(channel));
    }

    /**
     * Close all open channels.
     */
    @LuaFunction
    public final void closeAll() {
        state.closeAll();
    }

    /**
     * Sends a modem message on a certain channel. Modems listening on the channel will queue a {@code modem_message}
     * event on adjacent computers.
     * <p>
     * > [!NOTE]
     * > The channel does not need be open to send a message.
     *
     * @param channel      The channel to send messages on.
     * @param replyChannel The channel that responses to this message should be sent on. This can be the same as
     *                     {@code channel} or entirely different. The channel must have been {@link #open opened} on
     *                     the sending computer in order to receive the replies.
     * @param payload      The object to send. This can be any primitive type (boolean, number, string) as well as
     *                     tables. Other types (like functions), as well as metatables, will not be transmitted.
     * @throws LuaException If the channel is out of range.
     * @cc.usage Wrap a modem and a message on channel 15, requesting a response on channel 43.
     *
     * <pre>{@code
     * local modem = peripheral.find("modem") or error("No modem attached", 0)
     * modem.transmit(15, 43, "Hello, world!")
     * }</pre>
     */
    @LuaFunction
    public final void transmit(int channel, int replyChannel, Object payload) throws LuaException {
        parseChannel(channel);
        parseChannel(replyChannel);

        var world = getLevel();
        var position = getPosition();
        var network = this.network;

        if (world == null || position == null || network == null) return;

        var packet = new Packet(channel, replyChannel, payload, this);
        if (isInterdimensional()) {
            network.transmitInterdimensional(packet);
        } else {
            network.transmitSameDimension(packet, getRange());
        }
    }

    /**
     * Determine if this is a wired or wireless modem.
     * <p>
     * Some methods (namely those dealing with wired networks and remote peripherals) are only available on wired
     * modems.
     *
     * @return {@code true} if this is a wireless modem.
     */
    @LuaFunction
    public final boolean isWireless() {
        var network = this.network;
        return network != null && network.isWireless();
    }

    @Override
    public synchronized void attach(IComputerAccess computer) {
        synchronized (computers) {
            computers.add(computer);
        }

        setNetwork(getNetwork());
    }

    @Override
    public synchronized void detach(IComputerAccess computer) {
        boolean empty;
        synchronized (computers) {
            computers.remove(computer);
            empty = computers.isEmpty();
        }

        if (empty) setNetwork(null);
    }

    @Override
    public String getSenderID() {
        synchronized (computers) {
            if (computers.size() != 1) {
                return "unknown";
            } else {
                var computer = computers.iterator().next();
                return computer.getID() + "_" + computer.getAttachmentName();
            }
        }
    }
}
