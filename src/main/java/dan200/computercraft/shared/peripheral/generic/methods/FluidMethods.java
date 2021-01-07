/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.GenericSource;
import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.shared.peripheral.generic.methods.ArgumentHelpers.getRegistryEntry;

/**
 * Methods for interacting with tanks and other fluid storage blocks.
 *
 * @cc.module fluid_storage
 */
@AutoService( GenericSource.class )
public class FluidMethods implements GenericSource
{
    @Nonnull
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
            if( !stack.isEmpty() ) result.put( i + 1, FluidData.fillBasic( new HashMap<>( 4 ), stack ) );
        }

        return result;
    }

    @LuaFunction( mainThread = true )
    public static int pushFluid(
        IFluidHandler from, IComputerAccess computer,
        String toName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException
    {
        Fluid fluid = fluidName.isPresent()
            ? getRegistryEntry( fluidName.get(), "fluid", ForgeRegistries.FLUIDS )
            : null;

        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( toName );
        if( location == null ) throw new LuaException( "Target '" + toName + "' does not exist" );

        IFluidHandler to = extractHandler( location.getTarget() );
        if( to == null ) throw new LuaException( "Target '" + toName + "' is not an tank" );

        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        if( actualLimit <= 0 ) throw new LuaException( "Limit must be > 0" );

        return fluid == null
            ? moveFluid( from, actualLimit, to )
            : moveFluid( from, new FluidStack( fluid, actualLimit ), to );
    }

    @LuaFunction( mainThread = true )
    public static int pullFluid(
        IFluidHandler to, IComputerAccess computer,
        String fromName, Optional<Integer> limit, Optional<String> fluidName
    ) throws LuaException
    {
        Fluid fluid = fluidName.isPresent()
            ? getRegistryEntry( fluidName.get(), "fluid", ForgeRegistries.FLUIDS )
            : null;

        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( fromName );
        if( location == null ) throw new LuaException( "Target '" + fromName + "' does not exist" );

        IFluidHandler from = extractHandler( location.getTarget() );
        if( from == null ) throw new LuaException( "Target '" + fromName + "' is not an tank" );

        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        if( actualLimit <= 0 ) throw new LuaException( "Limit must be > 0" );

        return fluid == null
            ? moveFluid( from, actualLimit, to )
            : moveFluid( from, new FluidStack( fluid, actualLimit ), to );
    }

    @Nullable
    private static IFluidHandler extractHandler( @Nullable Object object )
    {
        if( object instanceof ICapabilityProvider )
        {
            LazyOptional<IFluidHandler> cap = ((ICapabilityProvider) object).getCapability( CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY );
            if( cap.isPresent() ) return cap.orElseThrow( NullPointerException::new );
        }

        if( object instanceof IFluidHandler ) return (IFluidHandler) object;
        return null;
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from  The handler to move from.
     * @param limit The maximum amount of fluid to move.
     * @param to    The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid( IFluidHandler from, int limit, IFluidHandler to )
    {
        return moveFluid( from, from.drain( limit, IFluidHandler.FluidAction.SIMULATE ), limit, to );
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from  The handler to move from.
     * @param fluid The fluid and limit to move.
     * @param to    The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid( IFluidHandler from, FluidStack fluid, IFluidHandler to )
    {
        return moveFluid( from, from.drain( fluid, IFluidHandler.FluidAction.SIMULATE ), fluid.getAmount(), to );
    }

    /**
     * Move fluid from one handler to another.
     *
     * @param from      The handler to move from.
     * @param extracted The fluid which is extracted from {@code from}.
     * @param limit     The maximum amount of fluid to move.
     * @param to        The handler to move to.
     * @return The amount of fluid moved.
     */
    private static int moveFluid( IFluidHandler from, FluidStack extracted, int limit, IFluidHandler to )
    {
        if( extracted == null || extracted.getAmount() <= 0 ) return 0;

        // Limit the amount to extract.
        extracted = extracted.copy();
        extracted.setAmount( Math.min( extracted.getAmount(), limit ) );

        int inserted = to.fill( extracted.copy(), IFluidHandler.FluidAction.EXECUTE );
        if( inserted <= 0 ) return 0;

        // Remove the item from the original inventory. Technically this could fail, but there's little we can do
        // about that.
        extracted.setAmount( inserted );
        from.drain( extracted, IFluidHandler.FluidAction.EXECUTE );
        return inserted;
    }
}
