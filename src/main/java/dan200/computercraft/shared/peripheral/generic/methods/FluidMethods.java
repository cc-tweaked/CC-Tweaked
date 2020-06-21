/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.asm.GenericSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@AutoService( GenericSource.class )
public class FluidMethods implements GenericSource
{
    @Override
    public ResourceLocation id()
    {
        return new ResourceLocation( ForgeVersion.MOD_ID, "fluid" );
    }

    @LuaFunction( mainThread = true )
    public static Map<Integer, Map<String, ?>> tanks( IFluidHandler fluids )
    {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        int size = fluids.getTanks();
        for( int i = 0; i < size; i++ )
        {
            FluidStack stack = fluids.getFluidInTank( i );
            if( !stack.isEmpty() ) result.put( i + 1, fillBasicMeta( new HashMap<>( 4 ), stack ) );
        }

        return result;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasicMeta( @Nonnull T data, @Nonnull FluidStack stack )
    {
        data.put( "name", Objects.toString( stack.getFluid().getRegistryName() ) );
        data.put( "amount", stack.getAmount() );
        return data;
    }
}
