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
 * @see ComputerInput#paste(String)"
 */
public class PasteEventComputerMessage extends ComputerServerMessage {
    private final String text;

    public PasteEventComputerMessage(AbstractContainerMenu menu, String text) {
        super(menu);
        this.text = text;
    }

    public PasteEventComputerMessage(FriendlyByteBuf buf) {
        super(buf);
        this.text = buf.readUtf(StringUtil.MAX_PASTE_LENGTH);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeUtf(text, StringUtil.MAX_PASTE_LENGTH);
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
