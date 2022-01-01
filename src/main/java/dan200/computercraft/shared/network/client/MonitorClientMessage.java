/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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

    public MonitorClientMessage( @Nonnull FriendlyByteBuf buf )
    {
        pos = buf.readBlockPos();
        state = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeBlockPos( pos );
        state.write( buf );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if( player == null || player.level == null ) return;

        BlockEntity te = player.level.getBlockEntity( pos );
        if( !(te instanceof TileMonitor) ) return;

        ((TileMonitor) te).read( state );
    }
}
