/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GenericPeripheralProvider
{
    @Nullable
    public static IPeripheral getPeripheral( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile == null ) return null;

        ArrayList<SaturatedMethod> saturated = new ArrayList<>( 0 );

        // This seems to add inventory methods, how???
        List<NamedMethod<PeripheralMethod>> tileMethods = PeripheralMethod.GENERATOR.getMethods( tile.getClass() );
        if( !tileMethods.isEmpty() ) addSaturated( saturated, tile, tileMethods );

        return saturated.isEmpty() ? null : new GenericPeripheral( tile, saturated );
    }

    private static void addSaturated( ArrayList<SaturatedMethod> saturated, Object target, List<NamedMethod<PeripheralMethod>> methods )
    {
        saturated.ensureCapacity( saturated.size() + methods.size() );
        for( NamedMethod<PeripheralMethod> method : methods )
        {
            saturated.add( new SaturatedMethod( target, method ) );
        }
    }
}
