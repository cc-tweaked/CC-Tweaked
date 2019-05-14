/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import dan200.computercraft.ComputerCraft;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixers.Schemas;
import net.minecraft.datafixers.TypeReferences;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.MutableRegistry;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NamedBlockEntityType<T extends BlockEntity> extends BlockEntityType<T>
{
    private final Identifier identifier;
    private Block block;

    private NamedBlockEntityType( Identifier identifier, Supplier<? extends T> supplier )
    {
        super( supplier, Collections.emptySet(), getDatafixer( identifier ) );
        this.identifier = identifier;
    }

    public static <T extends BlockEntity> NamedBlockEntityType<T> create( Identifier identifier, Supplier<? extends T> supplier )
    {
        return new NamedBlockEntityType<>( identifier, supplier );
    }

    public static <T extends BlockEntity> NamedBlockEntityType<T> create( Identifier identifier, Function<NamedBlockEntityType<T>, ? extends T> builder )
    {
        return new FixedPointSupplier<T>( identifier, builder ).factory;
    }

    public Identifier getId()
    {
        return identifier;
    }

    @Override
    public boolean supports( Block block )
    {
        return block == this.block;
    }

    public void setBlock( @Nonnull Block block )
    {
        if( this.block != null ) throw new IllegalStateException( "Cannot override block" );
        this.block = Objects.requireNonNull( block, "block cannot be null" );
    }

    public void register( MutableRegistry<BlockEntityType<?>> registry )
    {
        registry.add( getId(), this );
    }

    public static Type<?> getDatafixer( Identifier id )
    {
        try
        {
            return Schemas.getFixer()
                .getSchema( DataFixUtils.makeKey( ComputerCraft.DATAFIXER_VERSION ) )
                .getChoiceType( TypeReferences.BLOCK_ENTITY, id.toString() );
        }
        catch( IllegalArgumentException e )
        {
            if( SharedConstants.isDevelopment ) throw e;
            ComputerCraft.log.warn( "No data fixer registered for block entity " + id );
            return null;
        }
    }

    private static final class FixedPointSupplier<T extends BlockEntity> implements Supplier<T>
    {
        final NamedBlockEntityType<T> factory;
        private final Function<NamedBlockEntityType<T>, ? extends T> builder;

        private FixedPointSupplier( Identifier identifier, Function<NamedBlockEntityType<T>, ? extends T> builder )
        {
            factory = create( identifier, this );
            this.builder = builder;
        }

        @Override
        public T get()
        {
            return builder.apply( factory );
        }
    }
}
