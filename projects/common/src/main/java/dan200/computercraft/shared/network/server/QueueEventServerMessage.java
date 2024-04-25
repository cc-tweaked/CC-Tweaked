// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * Queue an event on a {@link ServerComputer}.
 *
 * @see ServerInputHandler#queueEvent(String)
 */
public final class QueueEventServerMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, QueueEventServerMessage> STREAM_CODEC = StreamCodec.ofMember(QueueEventServerMessage::write, QueueEventServerMessage::new);

    private final String event;
    private final @Nullable Object[] args;

    public QueueEventServerMessage(AbstractContainerMenu menu, String event, @Nullable Object[] args) {
        super(menu.containerId);
        this.event = event;
        this.args = args;
    }

    private QueueEventServerMessage(FriendlyByteBuf buf) {
        super(buf.readVarInt());
        event = buf.readUtf(Short.MAX_VALUE);

        var args = buf.readNbt();
        this.args = args == null ? null : NBTUtil.decodeObjects(args);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId());
        buf.writeUtf(event);
        buf.writeNbt(args == null ? null : NBTUtil.encodeObjects(args));
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        container.getInput().queueEvent(event, args);
    }

    @Override
    public CustomPacketPayload.Type<QueueEventServerMessage> type() {
        return NetworkMessages.QUEUE_EVENT;
    }
}
