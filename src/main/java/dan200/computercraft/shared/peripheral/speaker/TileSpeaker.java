/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileSpeaker extends TileGeneric implements ITickableTileEntity
{
    private final SpeakerPeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;
    private final UUID source = UUID.randomUUID();

    public TileSpeaker( TileEntityType<TileSpeaker> type )
    {
        super( type );
        peripheral = new Peripheral( this );
    }

    @Override
    public void tick()
    {
        peripheral.update();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        NetworkHandler.sendToAllPlayers( new SpeakerStopClientMessage( source ) );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( peripheralCap == null ) peripheralCap = LazyOptional.of( () -> peripheral );
            return peripheralCap.cast();
        }

        return super.getCapability( cap, side );
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        peripheralCap = CapabilityUtil.invalidate( peripheralCap );
    }

    private static final class Peripheral extends SpeakerPeripheral
    {
        private final TileSpeaker speaker;

        private Peripheral( TileSpeaker speaker )
        {
            this.speaker = speaker;
        }

        @Override
        public World getWorld()
        {
            return speaker.getLevel();
        }

        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = speaker.getBlockPos();
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        protected UUID getSource()
        {
            return speaker.source;
        }

        @Override
        public boolean equals( @Nullable IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && speaker == ((Peripheral) other).speaker);
        }
    }
}
