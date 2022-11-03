/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class MonitorClientMessage implements NetworkMessage {
    private final BlockPos pos;
    private final TerminalState state;

    public MonitorClientMessage(BlockPos pos, TerminalState state) {
        this.pos = pos;
        this.state = state;
    }

    public MonitorClientMessage(@Nonnull FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        state = new TerminalState(buf);
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        state.write(buf);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(NetworkEvent.Context context) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level == null) return;

        var te = player.level.getBlockEntity(pos);
        if (!(te instanceof TileMonitor)) return;

        ((TileMonitor) te).read(state);
    }
}
