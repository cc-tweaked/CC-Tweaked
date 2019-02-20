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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Objects;

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
        this.soundEvent = event;
    }

    public PlayRecordClientMessage( BlockPos pos )
    {
        this.pos = pos;
        this.name = null;
        this.soundEvent = null;
    }

    public PlayRecordClientMessage( PacketBuffer buf )
    {
        pos = buf.readBlockPos();
        if( buf.readBoolean() )
        {
            name = buf.readString( Short.MAX_VALUE );
            soundEvent = ForgeRegistries.SOUND_EVENTS.getValue( buf.readResourceLocation() );
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
            buf.writeString( name );
            buf.writeResourceLocation( Objects.requireNonNull( soundEvent.getRegistryName(), "Sound is not registered" ) );
        }
    }

    @Override
    @OnlyIn( Dist.CLIENT )
    public void handle( NetworkEvent.Context context )
    {
        Minecraft mc = Minecraft.getInstance();
        mc.world.playRecord( pos, soundEvent );
        if( name != null ) mc.ingameGUI.setRecordPlayingMessage( name );
    }
}
