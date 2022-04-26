/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

/**
 * Starts or stops a record on the client, depending on if {@link #soundEvent} is {@code null}.
 *
 * Used by disk drives to play record items.
 *
 * @see dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive
 */
public class PlayRecordClientMessage implements NetworkMessage
{
    private final BlockPos pos;
    private final String name;
    private final SoundEvent soundEvent;

    public PlayRecordClientMessage( BlockPos pos, SoundEvent event, String name )
    {
        this.pos = pos;
        this.name = name;
        soundEvent = event;
    }

    public PlayRecordClientMessage( BlockPos pos )
    {
        this.pos = pos;
        name = null;
        soundEvent = null;
    }

    public PlayRecordClientMessage( PacketBuffer buf )
    {
        pos = buf.readBlockPos();
        if( buf.readBoolean() )
        {
            name = buf.readUtf( Short.MAX_VALUE );
            soundEvent = buf.readRegistryIdSafe( SoundEvent.class );
        }
        else
        {
            name = null;
            soundEvent = null;
        }
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
            buf.writeUtf( name );
            buf.writeRegistryId( soundEvent );
        }
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        Minecraft mc = Minecraft.getInstance();
        mc.levelRenderer.playRecord( soundEvent, pos, null );
        if( name != null ) mc.gui.setNowPlaying( new StringTextComponent( name ) );
    }
}
