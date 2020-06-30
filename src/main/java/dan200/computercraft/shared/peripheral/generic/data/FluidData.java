/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class FluidData
{
    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasic( @Nonnull T data, @Nonnull FluidStack stack )
    {
        data.put( "name", Objects.toString( stack.getFluid().getRegistryName() ) );
        data.put( "amount", stack.getAmount() );
        return data;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fill( @Nonnull T data, @Nonnull FluidStack stack )
    {
        fillBasic( data, stack );
        data.put( "tags", DataHelpers.getTags( stack.getFluid().getTags() ) );
        return data;
    }
}
