/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GenericPeripheralProvider
{
    private static final Capability<?>[] CAPABILITIES = new Capability<?>[] {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
        CapabilityEnergy.ENERGY,
        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
    };

    @Nullable
    public static IPeripheral getPeripheral( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side, NonNullConsumer<LazyOptional<IPeripheral>> invalidate )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile == null ) return null;

        ArrayList<SaturatedMethod> saturated = new ArrayList<>( 0 );

        List<NamedMethod<PeripheralMethod>> tileMethods = PeripheralMethod.GENERATOR.getMethods( tile.getClass() );
        if( !tileMethods.isEmpty() ) addSaturated( saturated, tile, tileMethods );

        for( Capability<?> capability : CAPABILITIES )
        {
            LazyOptional<?> wrapper = tile.getCapability( capability );
            wrapper.ifPresent( contents -> {
                List<NamedMethod<PeripheralMethod>> capabilityMethods = PeripheralMethod.GENERATOR.getMethods( contents.getClass() );
                if( capabilityMethods.isEmpty() ) return;

                addSaturated( saturated, contents, capabilityMethods );
                wrapper.addListener( cast( invalidate ) );
            } );
        }

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

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static <T> NonNullConsumer<T> cast( NonNullConsumer<?> consumer )
    {
        return (NonNullConsumer) consumer;
    }
}
