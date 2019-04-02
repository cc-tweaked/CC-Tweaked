/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import dan200.computercraft.ComputerCraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.TypeReferences;

import java.util.function.Function;
import java.util.function.Supplier;

public final class NamedBlockEntityType<T extends TileEntity> extends TileEntityType<T>
{
    private final ResourceLocation identifier;

    private NamedBlockEntityType( ResourceLocation identifier, Supplier<? extends T> supplier )
    {
        super( supplier, getDatafixer( identifier ) );
        this.identifier = identifier;
        setRegistryName( identifier );
    }

    public static <T extends TileEntity> NamedBlockEntityType<T> create( ResourceLocation identifier, Supplier<? extends T> supplier )
    {
        return new NamedBlockEntityType<>( identifier, supplier );
    }

    public static <T extends TileEntity> NamedBlockEntityType<T> create( ResourceLocation identifier, Function<NamedBlockEntityType<T>, ? extends T> builder )
    {
        return new FixedPointSupplier<>( identifier, builder ).factory;
    }

    public ResourceLocation getId()
    {
        return identifier;
    }

    public static Type<?> getDatafixer( ResourceLocation id )
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
        final NamedBlockEntityType<T> factory;
        private final Function<NamedBlockEntityType<T>, ? extends T> builder;

        private FixedPointSupplier( ResourceLocation identifier, Function<NamedBlockEntityType<T>, ? extends T> builder )
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
