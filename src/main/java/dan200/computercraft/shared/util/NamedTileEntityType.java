/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import dan200.computercraft.ComputerCraft;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.TypeReferences;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NamedTileEntityType<T extends TileEntity> extends TileEntityType<T>
{
    private final ResourceLocation identifier;
    private Block block;

    private NamedTileEntityType( ResourceLocation identifier, Supplier<? extends T> supplier )
    {
        super( supplier, Collections.emptySet(), getDatafixer( identifier ) );
        this.identifier = identifier;
        setRegistryName( identifier );
    }

    public static <T extends TileEntity> NamedTileEntityType<T> create( ResourceLocation identifier, Supplier<? extends T> supplier )
    {
        return new NamedTileEntityType<>( identifier, supplier );
    }

    public static <T extends TileEntity> NamedTileEntityType<T> create( ResourceLocation identifier, Function<NamedTileEntityType<T>, ? extends T> builder )
    {
        return new FixedPointSupplier<>( identifier, builder ).factory;
    }

    public void setBlock( @Nonnull Block block )
    {
        if( this.block != null ) throw new IllegalStateException( "Cannot change block once set" );
        this.block = Objects.requireNonNull( block, "block cannot be null" );
    }

    @Override
    public boolean isValidBlock( @Nonnull Block block )
    {
        return block == this.block;
    }

    public ResourceLocation getId()
    {
        return identifier;
    }

    private static Type<?> getDatafixer( ResourceLocation id )
    {
        try
        {
            return DataFixesManager.getDataFixer()
                .getSchema( DataFixUtils.makeKey( ComputerCraft.DATAFIXER_VERSION ) )
                .getChoiceType( TypeReferences.BLOCK_ENTITY, id.toString() );
        }
        catch( IllegalArgumentException e )
        {
            if( SharedConstants.developmentMode ) throw e;
            ComputerCraft.log.warn( "No data fixer registered for block entity " + id );
            return null;
        }
    }

    private static final class FixedPointSupplier<T extends TileEntity> implements Supplier<T>
    {
        final NamedTileEntityType<T> factory;
        private final Function<NamedTileEntityType<T>, ? extends T> builder;

        private FixedPointSupplier( ResourceLocation identifier, Function<NamedTileEntityType<T>, ? extends T> builder )
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
