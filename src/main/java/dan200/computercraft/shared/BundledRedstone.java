/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;

public final class BundledRedstone
{
    private static final ArrayList<IBundledRedstoneProvider> providers = new ArrayList<>();

    private BundledRedstone() {}

    public static synchronized void register( @Nonnull IBundledRedstoneProvider provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );
        if( !providers.contains( provider ) ) providers.add( provider );
    }

    public static int getDefaultOutput( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        return world.isInWorldBounds( pos ) ? DefaultBundledRedstoneProvider.getDefaultBundledRedstoneOutput( world, pos, side ) : -1;
    }

    private static int getUnmaskedOutput( Level world, BlockPos pos, Direction side )
    {
        if( !world.isInWorldBounds( pos ) ) return -1;

        // Try the providers in order:
        int combinedSignal = -1;
        for( IBundledRedstoneProvider bundledRedstoneProvider : providers )
        {
            try
            {
                int signal = bundledRedstoneProvider.getBundledRedstoneOutput( world, pos, side );
                if( signal >= 0 )
                {
                    combinedSignal = combinedSignal < 0 ? signal & 0xffff : combinedSignal | (signal & 0xffff);
                }
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Bundled redstone provider " + bundledRedstoneProvider + " errored.", e );
            }
        }

        return combinedSignal;
    }

    public static int getOutput( Level world, BlockPos pos, Direction side )
    {
        int signal = getUnmaskedOutput( world, pos, side );
        return signal >= 0 ? signal : 0;
    }
}
