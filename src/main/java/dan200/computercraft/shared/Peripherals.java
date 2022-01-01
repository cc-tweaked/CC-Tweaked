/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public final class Peripherals
{
    private static final Collection<IPeripheralProvider> providers = new LinkedHashSet<>();

    private Peripherals() {}

    public static synchronized void register( @Nonnull IPeripheralProvider provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );
        providers.add( provider );
    }

    @Nullable
    public static IPeripheral getPeripheral( Level world, BlockPos pos, Direction side, NonNullConsumer<Object> invalidate )
    {
        return world.isInWorldBounds( pos ) && !world.isClientSide ? getPeripheralAt( world, pos, side, invalidate ) : null;
    }

    @Nullable
    private static IPeripheral getPeripheralAt( Level world, BlockPos pos, Direction side, NonNullConsumer<? super Object> invalidate )
    {
        BlockEntity block = world.getBlockEntity( pos );
        if( block != null )
        {
            LazyOptional<IPeripheral> peripheral = block.getCapability( CAPABILITY_PERIPHERAL, side );
            if( peripheral.isPresent() ) return CapabilityUtil.unwrap( peripheral, invalidate );
        }

        // Try the handlers in order:
        for( IPeripheralProvider peripheralProvider : providers )
        {
            try
            {
                LazyOptional<IPeripheral> peripheral = peripheralProvider.getPeripheral( world, pos, side );
                if( peripheral.isPresent() ) return CapabilityUtil.unwrap( peripheral, invalidate );
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Peripheral provider " + peripheralProvider + " errored.", e );
            }
        }

        return GenericPeripheralProvider.getPeripheral( world, pos, side, invalidate );
    }

}
