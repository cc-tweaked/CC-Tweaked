// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.core.util.StringUtil;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.nio.ByteBuffer;

/**
 * Paste a string on a {@link ServerComputer}.
 *
 * @see ServerInputHandler#paste(ByteBuffer)
 */
public class PasteEventComputerMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, PasteEventComputerMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PasteEventComputerMessage::containerId,
        MoreStreamCodecs.byteBuffer(StringUtil.MAX_PASTE_LENGTH), c -> c.text,
        PasteEventComputerMessage::new
    );

    private final ByteBuffer text;

    public PasteEventComputerMessage(AbstractContainerMenu menu, ByteBuffer text) {
        this(menu.containerId, text);
    }

    private PasteEventComputerMessage(int id, ByteBuffer text) {
        super(id);
        this.text = text;
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        container.getInput().paste(text);
    }

    @Override
    public CustomPacketPayload.Type<PasteEventComputerMessage> type() {
        return NetworkMessages.PASTE_EVENT;
    }
}
