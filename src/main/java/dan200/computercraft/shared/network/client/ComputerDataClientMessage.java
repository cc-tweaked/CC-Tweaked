/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 */
public class ComputerDataClientMessage extends ComputerClientMessage {
    private ComputerState state;
    private CompoundTag userData;

    public ComputerDataClientMessage(ServerComputer computer) {
        super(computer.getInstanceID());
        this.state = computer.getState();
        this.userData = computer.getUserData();
    }

    public ComputerDataClientMessage() {
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        super.toBytes(buf);
        buf.writeEnumConstant(this.state);
        buf.writeCompoundTag(this.userData);
    }

    @Override
    public void fromBytes(@Nonnull PacketByteBuf buf) {
        super.fromBytes(buf);
        this.state = buf.readEnumConstant(ComputerState.class);
        this.userData = buf.readCompoundTag();
    }

    @Override
    public void handle(PacketContext context) {
        this.getComputer().setState(this.state, this.userData);
    }
}
