/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class MonitorClientMessage implements NetworkMessage
{
    private final BlockPos pos;
    private final TerminalState state;

    public MonitorClientMessage( BlockPos pos, TerminalState state )
    {
        this.pos = pos;
        this.state = state;
    }

    public MonitorClientMessage( @Nonnull PacketByteBuf buf )
    {
        pos = buf.readBlockPos();
        state = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeBlockPos( pos );
        state.write( buf );
    }

    @Override
    public void handle( PacketContext context )
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if( player == null || player.world == null ) return;

        BlockEntity te = player.world.getBlockEntity( pos );
        if( !(te instanceof TileMonitor) ) return;

        ((TileMonitor) te).read( state );
    }
}
