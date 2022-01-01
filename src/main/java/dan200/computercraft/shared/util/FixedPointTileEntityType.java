/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * A {@link BlockEntityType} whose supplier uses itself as an argument.
 *
 * @param <T> The type of the produced tile entity.
 */
public final class FixedPointTileEntityType<T extends BlockEntity> extends BlockEntityType<T>
{
    private final Supplier<? extends Block> block;

    private FixedPointTileEntityType( Supplier<? extends Block> block, BlockEntitySupplier<T> builder )
    {
        super( builder, Collections.emptySet(), null );
        this.block = block;
    }

    public static <T extends BlockEntity> FixedPointTileEntityType<T> create( Supplier<? extends Block> block, FixedPointBlockEntitySupplier<T> builder )
    {
        return new FixedPointSupplier<>( block, builder ).factory;
    }

    @Override
    public boolean isValid( @Nonnull BlockState block )
    {
        return block.getBlock() == this.block.get();
    }

    public Block getBlock()
    {
        return block.get();
    }

    private static final class FixedPointSupplier<T extends BlockEntity> implements BlockEntitySupplier<T>
    {
        final FixedPointTileEntityType<T> factory;
        private final FixedPointBlockEntitySupplier<T> builder;

        private FixedPointSupplier( Supplier<? extends Block> block, FixedPointBlockEntitySupplier<T> builder )
        {
            factory = new FixedPointTileEntityType<>( block, this );
            this.builder = builder;
        }

        @Nonnull
        @Override
        public T create( @Nonnull BlockPos pos, @Nonnull BlockState state )
        {
            return builder.create( factory, pos, state );
        }
    }

    @FunctionalInterface
    public interface FixedPointBlockEntitySupplier<T extends BlockEntity>
    {
        T create( BlockEntityType<T> type, BlockPos pos, BlockState state );
    }
}
