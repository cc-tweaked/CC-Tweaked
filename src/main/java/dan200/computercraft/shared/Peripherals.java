/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class Peripherals
{
    private static final Collection<IPeripheralProvider> providers = new LinkedHashSet<>();

    private Peripherals() {}

    public static synchronized void register( @Nonnull IPeripheralProvider provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );
        providers.add( provider );
    }

    public static IPeripheral getPeripheral( World world, BlockPos pos, Direction side )
    {
        return World.isValid( pos ) && !world.isRemote ? getPeripheralAt( world, pos, side ) : null;
    }

    private static IPeripheral getPeripheralAt( World world, BlockPos pos, Direction side )
    {
        // Try the handlers in order:
        for( IPeripheralProvider peripheralProvider : providers )
        {
            try
            {
                IPeripheral peripheral = peripheralProvider.getPeripheral( world, pos, side );
                if( peripheral != null ) return peripheral;
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Peripheral provider " + peripheralProvider + " errored.", e );
            }
        }

        return null;
    }

}
