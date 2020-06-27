/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.meta;

import com.google.gson.JsonParseException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemMeta
{
    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasicMeta( @Nonnull T data, @Nonnull ItemStack stack )
    {
        data.put( "name", Objects.toString( stack.getItem().getRegistryName() ) );
        data.put( "count", stack.getCount() );
        return data;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fillMeta( @Nonnull T data, @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return data;

        fillBasicMeta( data, stack );

        data.put( "displayName", stack.getDisplayName().getString() );
        data.put( "rawName", stack.getTranslationKey() );
        data.put( "maxCount", stack.getMaxStackSize() );

        if( stack.isDamageable() )
        {
            data.put( "damage", stack.getDamage() );
            data.put( "maxDamage", stack.getMaxDamage() );
        }

        if( stack.getItem().showDurabilityBar( stack ) )
        {
            data.put( "durability", stack.getItem().getDurabilityForDisplay( stack ) );
        }

        CompoundNBT tag = stack.getTag();
        if( tag != null && tag.contains( "display", Constants.NBT.TAG_COMPOUND ) )
        {
            CompoundNBT displayTag = tag.getCompound( "display" );
            if( displayTag.contains( "Lore", Constants.NBT.TAG_LIST ) )
            {
                ListNBT loreTag = displayTag.getList( "Lore", Constants.NBT.TAG_STRING );
                data.put( "lore", loreTag.stream()
                    .map( ItemMeta::parseTextComponent )
                    .filter( Objects::nonNull )
                    .map( ITextComponent::getString )
                    .collect( Collectors.toList() ) );
            }
        }

        return data;
    }

    @Nullable
    private static ITextComponent parseTextComponent( @Nonnull INBT x )
    {
        try
        {
            return ITextComponent.Serializer.fromJson( x.getString() );
        }
        catch( JsonParseException e )
        {
            return null;
        }
    }
}
