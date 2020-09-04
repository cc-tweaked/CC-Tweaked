/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import javax.annotation.Nonnull;

import net.minecraft.network.PacketByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

public class ComputerTerminalClientMessage extends ComputerClientMessage {
    private TerminalState state;

    public ComputerTerminalClientMessage(int instanceId, TerminalState state) {
        super(instanceId);
        this.state = state;
    }

    public ComputerTerminalClientMessage() {
    }

    @Override
    public void toBytes(@Nonnull PacketByteBuf buf) {
        super.toBytes(buf);
        this.state.write(buf);
    }

    @Override
    public void fromBytes(@Nonnull PacketByteBuf buf) {
        super.fromBytes(buf);
        this.state = new TerminalState(buf);
    }

    @Override
    public void handle(PacketContext context) {
        this.getComputer().read(this.state);
    }
}
