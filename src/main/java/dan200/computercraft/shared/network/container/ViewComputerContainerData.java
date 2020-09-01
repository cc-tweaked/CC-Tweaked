/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ServerComputer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * View an arbitrary computer on the client.
 *
 * @see dan200.computercraft.shared.command.CommandComputerCraft
 */
public class ViewComputerContainerData extends ComputerContainerData {
    private int width;
    private int height;

    public ViewComputerContainerData(ServerComputer computer) {
        super(computer);
        Terminal terminal = computer.getTerminal();
        if (terminal != null) {
            width = terminal.getWidth();
            height = terminal.getHeight();
        } else {
            width = height = 0;
        }
    }

    public ViewComputerContainerData(PacketByteBuf packetByteBuf) {
        super(packetByteBuf);
        toBytes(packetByteBuf);
    }

    private static final Identifier IDENTIFIER = new Identifier(ComputerCraft.MOD_ID, "viewComputerContainerData");

    @Override
    public Identifier getId() {
        return IDENTIFIER;
    }

    @Override
    public void fromBytes(PacketByteBuf buf) {
        super.fromBytes(buf);
        width = buf.readVarInt();
        height = buf.readVarInt();
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        super.toBytes(buf);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
