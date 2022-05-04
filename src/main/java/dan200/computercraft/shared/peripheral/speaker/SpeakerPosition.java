/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;

public final class SpeakerPosition
{
    private final World level;
    private final Vector3d position;
    private final Entity entity;

    private SpeakerPosition( @Nullable World level, @Nonnull Vector3d position, @Nullable Entity entity )
    {
        this.level = level;
        this.position = position;
        this.entity = entity;
    }

    public static SpeakerPosition of( @Nullable World level, @Nonnull Vector3d position )
    {
        return new SpeakerPosition( level, position, null );
    }

    public static SpeakerPosition of( @Nonnull Entity entity )
    {
        return new SpeakerPosition( entity.level, entity.getEyePosition( 1 ), entity );
    }

    @Nullable
    public World level()
    {
        return level;
    }

    @Nonnull
    public Vector3d position()
    {
        return position;
    }

    @Nullable
    public Entity entity()
    {
        return entity;
    }

    public boolean withinDistance( SpeakerPosition other, double distanceSq )
    {
        return level == other.level && entity == other.entity && position.distanceToSqr( other.position ) <= distanceSq;
    }

    public Message asMessage()
    {
        if( level == null ) throw new NullPointerException( "Cannot send a position without a level" );
        return new Message( level.dimension().getRegistryName(), position, entity == null ? OptionalInt.empty() : OptionalInt.of( entity.getId() ) );
    }

    public static final class Message
    {
        private final ResourceLocation level;
        private final Vector3d position;
        private final OptionalInt entity;

        private Message( ResourceLocation level, Vector3d position, OptionalInt entity )
        {
            this.level = level;
            this.position = position;
            this.entity = entity;
        }

        public static Message read( @Nonnull PacketBuffer buffer )
        {
            ResourceLocation level = buffer.readResourceLocation();
            Vector3d position = new Vector3d( buffer.readDouble(), buffer.readDouble(), buffer.readDouble() );
            OptionalInt entity = buffer.readBoolean() ? OptionalInt.of( buffer.readInt() ) : OptionalInt.empty();
            return new Message( level, position, entity );
        }

        public void write( @Nonnull PacketBuffer buffer )
        {
            buffer.writeResourceLocation( level );

            buffer.writeDouble( position.x );
            buffer.writeDouble( position.y );
            buffer.writeDouble( position.z );

            buffer.writeBoolean( entity.isPresent() );
            if( entity.isPresent() ) buffer.writeInt( entity.getAsInt() );
        }

        @Nonnull
        @OnlyIn( Dist.CLIENT )
        public SpeakerPosition reify()
        {
            Minecraft minecraft = Minecraft.getInstance();
            World level = minecraft.level;
            if( level != null && !level.dimension().getRegistryName().equals( this.level ) ) level = null;

            return new SpeakerPosition(
                level, position,
                level != null && entity.isPresent() ? level.getEntity( entity.getAsInt() ) : null
            );
        }
    }
}
