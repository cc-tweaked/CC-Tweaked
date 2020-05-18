/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nonnull;

public class MonitorClientMessage implements NetworkMessage
{
    private BlockPos pos;
    private TerminalState state;

    public MonitorClientMessage( BlockPos pos, TerminalState state )
    {
        this.pos = pos;
        this.state = state;
    }

    public MonitorClientMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeBlockPos( pos );
        state.write( buf );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        pos = buf.readBlockPos();
        state = new TerminalState( buf );
    }

    @Override
    public void handle( MessageContext context )
    {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if( player == null || player.world == null ) return;

        TileEntity te = player.world.getTileEntity( pos );
        if( !(te instanceof TileMonitor) ) return;

        ((TileMonitor) te).read( state );
    }
}
