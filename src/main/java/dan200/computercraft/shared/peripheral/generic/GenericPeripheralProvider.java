/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.core.asm.PeripheralMethod;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GenericPeripheralProvider
{
    private static final ArrayList<Capability<?>> capabilities = new ArrayList<>();

    public static synchronized void addCapability( Capability<?> capability )
    {
        Objects.requireNonNull( capability, "Capability cannot be null" );
        if( !capabilities.contains( capability ) ) capabilities.add( capability );
    }

    @Nullable
    public static IPeripheral getPeripheral( @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Direction side, NonNullConsumer<Object> invalidate )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile == null ) return null;

        GenericPeripheralBuilder saturated = new GenericPeripheralBuilder();

        List<NamedMethod<PeripheralMethod>> tileMethods = PeripheralMethod.GENERATOR.getMethods( tile.getClass() );
        if( !tileMethods.isEmpty() ) saturated.addMethods( tile, tileMethods );

        for( Capability<?> capability : capabilities )
        {
            LazyOptional<?> wrapper = tile.getCapability( capability );
            wrapper.ifPresent( contents -> {
                List<NamedMethod<PeripheralMethod>> capabilityMethods = PeripheralMethod.GENERATOR.getMethods( contents.getClass() );
                if( capabilityMethods.isEmpty() ) return;

                saturated.addMethods( contents, capabilityMethods );
                CapabilityUtil.addListener( wrapper, invalidate );
            } );
        }

        return saturated.toPeripheral( tile );
    }

    private static class GenericPeripheralBuilder
    {
        private String name;
        private final Set<String> additionalTypes = new HashSet<>( 0 );
        private final ArrayList<SaturatedMethod> methods = new ArrayList<>( 0 );

        IPeripheral toPeripheral( BlockEntity tile )
        {
            if( methods.isEmpty() ) return null;

            methods.trimToSize();
            return new GenericPeripheral( tile, name, additionalTypes, methods );
        }

        void addMethods( Object target, List<NamedMethod<PeripheralMethod>> methods )
        {
            ArrayList<SaturatedMethod> saturatedMethods = this.methods;
            saturatedMethods.ensureCapacity( saturatedMethods.size() + methods.size() );
            for( NamedMethod<PeripheralMethod> method : methods )
            {
                saturatedMethods.add( new SaturatedMethod( target, method ) );

                // If we have a peripheral type, use it. Always pick the smallest one, so it's consistent (assuming mods
                // don't change).
                PeripheralType type = method.getGenericType();
                if( type != null && type.getPrimaryType() != null )
                {
                    String name = type.getPrimaryType();
                    if( this.name == null || this.name.compareTo( name ) > 0 ) this.name = name;
                }
                if( type != null ) additionalTypes.addAll( type.getAdditionalTypes() );
            }
        }
    }
}
