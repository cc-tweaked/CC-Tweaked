/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Map;

public class FluidData
{
    public static void fillBasic( @Nonnull Map<? super String, Object> data, @Nonnull FluidStack stack )
    {
        data.put( "name", DataHelpers.getId( stack.getFluid() ) );
        data.put( "amount", stack.getAmount() );
    }

    public static void fill( @Nonnull Map<? super String, Object> data, @Nonnull FluidStack stack )
    {
        data.put( "tags", DataHelpers.getTags( stack.getFluid().getTags() ) );
    }
}
