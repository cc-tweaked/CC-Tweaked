/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link TileEntityType} whose supplier uses itself as an argument.
 *
 * @param <T> The type of the produced tile entity.
 */
public final class FixedPointTileEntityType<T extends TileEntity> extends TileEntityType<T>
{
    private final Supplier<? extends Block> block;

    private FixedPointTileEntityType( Supplier<? extends Block> block, Supplier<T> builder )
    {
        super( builder, Collections.emptySet(), null );
        this.block = block;
    }

    public static <T extends TileEntity> FixedPointTileEntityType<T> create( Supplier<? extends Block> block, Function<TileEntityType<T>, T> builder )
    {
        return new FixedPointSupplier<>( block, builder ).factory;
    }

    @Override
    public boolean isValidBlock( @Nonnull Block block )
    {
        return block == this.block.get();
    }

    private static final class FixedPointSupplier<T extends TileEntity> implements Supplier<T>
    {
        final FixedPointTileEntityType<T> factory;
        private final Function<TileEntityType<T>, T> builder;

        private FixedPointSupplier( Supplier<? extends Block> block, Function<TileEntityType<T>, T> builder )
        {
            factory = new FixedPointTileEntityType<>( block, this );
            this.builder = builder;
        }

        @Override
        public T get()
        {
            return builder.apply( factory );
        }
    }
}
