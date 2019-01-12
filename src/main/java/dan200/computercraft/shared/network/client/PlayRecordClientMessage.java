/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 * Starts or stops a record on the client, depending on if {@link #getSoundEvent()} is {@code null}.
 *
 * Used by disk drives to play record items.
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 */
public class PlayRecordClientMessage implements NetworkMessage
{
    private BlockPos pos;
    private String name;
    private SoundEvent soundEvent;

    public PlayRecordClientMessage( BlockPos pos, SoundEvent event, String name )
    {
        this.pos = pos;
        this.name = name;
        this.soundEvent = event;
    }

    public PlayRecordClientMessage( BlockPos pos )
    {
        this.pos = pos;
    }

    public PlayRecordClientMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeBlockPos( pos );
        if( soundEvent == null )
        {
            buf.writeBoolean( false );
        }
        else
        {
            buf.writeBoolean( true );
            buf.writeString( name );
            buf.writeInt( SoundEvent.REGISTRY.getIDForObject( soundEvent ) );
        }
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        pos = buf.readBlockPos();
        if( buf.readBoolean() )
        {
            name = buf.readString( Short.MAX_VALUE );
            soundEvent = SoundEvent.REGISTRY.getObjectById( buf.readInt() );
        }
    }

    @Override
    @SideOnly( Side.CLIENT )
    public void handle( MessageContext context )
    {
        Minecraft mc = Minecraft.getMinecraft();
        mc.world.playRecord( pos, soundEvent );
        if( name != null ) mc.ingameGUI.setRecordPlayingMessage( name );
    }
}
