/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ComputerContainerData implements ContainerData {
    private static final Identifier IDENTIFIER = new Identifier(ComputerCraft.MOD_ID, "computerContainerData");
    private int id;
    private ComputerFamily family;

    public ComputerContainerData(ServerComputer computer) {
        this.id = computer.getInstanceID();
        this.family = computer.getFamily();
    }

    @Override
    public Identifier getId() {
        return IDENTIFIER;
    }

    @Override
    public void fromBytes(PacketByteBuf buf) {
        this.id = buf.readInt();
        this.family = buf.readEnumConstant(ComputerFamily.class);
    }

    @Override
    public void toBytes(PacketByteBuf buf) {
        buf.writeInt(id);
        buf.writeEnumConstant(family);
    }

    public int getInstanceId() {
        return id;
    }

    public ComputerFamily getFamily() {
        return family;
    }
}
