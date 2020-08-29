/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * View an arbitrary computer on the client.
 *
 * @see dan200.computercraft.shared.command.CommandComputerCraft
 */
public class ViewComputerContainerType implements ContainerType<ContainerViewComputer> {
    public static final Identifier ID = new Identifier(ComputerCraft.MOD_ID, "view_computer_gui");

    public int instanceId;
    public int width;
    public int height;
    public ComputerFamily family;

    public ViewComputerContainerType(ServerComputer computer) {
        this.instanceId = computer.getInstanceID();
        Terminal terminal = computer.getTerminal();
        if (terminal != null) {
            this.width = terminal.getWidth();
            this.height = terminal.getHeight();
        }
        this.family = computer.getFamily();
    }

    public ViewComputerContainerType() {
    }

    @Override
    public void fromBytes(@Nonnull PacketByteBuf buf) {
        this.instanceId = buf.readVarInt();
        this.width = buf.readVarInt();
        this.height = buf.readVarInt();
        this.family = buf.readEnumConstant(ComputerFamily.class);
    }

    @Nonnull
    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        buf.writeVarInt(this.instanceId);
        buf.writeVarInt(this.width);
        buf.writeVarInt(this.height);
        buf.writeEnumConstant(this.family);
    }
}
