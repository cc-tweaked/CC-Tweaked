// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * Queue an event on a {@link ServerComputer}.
 *
 * @see ServerInputHandler#queueEvent(String)
 */
public class QueueEventServerMessage extends ComputerServerMessage {
    private final String event;
    private final @Nullable Object[] args;

    public QueueEventServerMessage(AbstractContainerMenu menu, String event, @Nullable Object[] args) {
        super(menu);
        this.event = event;
        this.args = args;
    }

    public QueueEventServerMessage(FriendlyByteBuf buf) {
        super(buf);
        event = buf.readUtf(Short.MAX_VALUE);

        var args = buf.readNbt();
        this.args = args == null ? null : NBTUtil.decodeObjects(args);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeUtf(event);
        buf.writeNbt(args == null ? null : NBTUtil.encodeObjects(args));
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        container.getInput().queueEvent(event, args);
    }

    @Override
    public MessageType<QueueEventServerMessage> type() {
        return NetworkMessages.QUEUE_EVENT;
    }
}
