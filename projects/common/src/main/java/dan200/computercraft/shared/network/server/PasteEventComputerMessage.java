// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.core.input.ComputerInput;
import dan200.computercraft.core.util.StringUtil;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessages;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.nio.ByteBuffer;

/**
 * Paste a string on a {@link ServerComputer}.
 *
 * @see ComputerInput#paste(ByteBuffer)
 */
public class PasteEventComputerMessage extends ComputerServerMessage {
    private final ByteBuffer text;

    public PasteEventComputerMessage(AbstractContainerMenu menu, ByteBuffer text) {
        super(menu);
        this.text = text;
    }

    public PasteEventComputerMessage(FriendlyByteBuf buf) {
        super(buf);

        var length = buf.readVarInt();
        if (length > StringUtil.MAX_PASTE_LENGTH) {
            throw new DecoderException("ByteArray with size " + length + " is bigger than allowed " + StringUtil.MAX_PASTE_LENGTH);
        }

        var text = new byte[length];
        buf.readBytes(text);
        this.text = ByteBuffer.wrap(text);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeVarInt(text.remaining());
        buf.writeBytes(text.duplicate());
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        container.getInput().getComputerInput().paste(text);
    }

    @Override
    public MessageType<PasteEventComputerMessage> type() {
        return NetworkMessages.PASTE_EVENT;
    }
}
