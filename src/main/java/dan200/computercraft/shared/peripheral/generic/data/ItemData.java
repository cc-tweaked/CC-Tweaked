/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.data;

import com.google.gson.JsonParseException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemData
{
    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasic( @Nonnull T data, @Nonnull ItemStack stack )
    {
        data.put( "name", Objects.toString( stack.getItem().getRegistryName() ) );
        data.put( "count", stack.getCount() );
        return data;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fill( @Nonnull T data, @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return data;

        fillBasic( data, stack );

        data.put( "displayName", stack.getDisplayName().getString() );
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

        data.put( "tags", DataHelpers.getTags( stack.getItem().getTags() ) );

        CompoundNBT tag = stack.getTag();
        if( tag != null && tag.contains( "display", Constants.NBT.TAG_COMPOUND ) )
        {
            CompoundNBT displayTag = tag.getCompound( "display" );
            if( displayTag.contains( "Lore", Constants.NBT.TAG_LIST ) )
            {
                ListNBT loreTag = displayTag.getList( "Lore", Constants.NBT.TAG_STRING );
                data.put( "lore", loreTag.stream()
                    .map( ItemData::parseTextComponent )
                    .filter( Objects::nonNull )
                    .map( ITextComponent::getString )
                    .collect( Collectors.toList() ) );
            }
        }


        /*
         * Used to hide some data from ItemStack tooltip.
         * @see https://minecraft.gamepedia.com/Tutorials/Command_NBT_tags
         * @see ItemStack.getTooltip(@ Nullable PlayerEntity playerIn, ITooltipFlag advanced)
         */
        int hideFlags = (tag != null) ? tag.getInt( "HideFlags" ) : 0;

        if ( stack.isEnchanted() )
        {
            List<Map<String, Object>> enchants;
            if ( (hideFlags & 1) == 0 )
            {
                /*
                 * Mimic the EnchantmentHelper.getEnchantments(ItemStack stack) behavior without special case for Enchanted book.
                 * I'll do that to have the same data than ones displayed in tooltip.
                 * @see EnchantmentHelper.getEnchantments(ItemStack stack)
                 */
                enchants = enchantMapToList( EnchantmentHelper.func_226652_a_( stack.getEnchantmentTagList() ) );
            }
            else
            {
                enchants = Collections.emptyList();
            }
            data.put( "enchantments", enchants );
        }

        /*if ( (hideFlags & 2) == 0 )
        {
            //TODO: Add attributesModifiers
        }*/

        if ( tag.getBoolean( "Unbreakable" ) && (hideFlags & 4) == 0 )
        {
            data.put( "unbreakable", true );
        }

        /*if ( (hideFlags & 8) == 0 )
        {
            //TODO: Add CanDestroy
        }*/

        /*if ( (hideFlags & 16) == 0 )
        {
            //TODO: Add Can place on
        }*/

        if ( (hideFlags & 32) == 0 )
        {
            // All other vanilla data should go here

            if ( stack.getItem() instanceof EnchantedBookItem )
            {
                data.put( "storedenchantments", enchantMapToList( EnchantmentHelper.func_226652_a_(
                    EnchantedBookItem.getEnchantments( stack )
                ) ) );
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

    @Nonnull
    private static List<Map<String, Object>> enchantMapToList( Map<Enchantment, Integer> rawEnchants )
    {
        List<Map<String, Object>> enchants = new ArrayList<>( rawEnchants.size() );
        rawEnchants.forEach( ( enchantment, level ) ->
        {
            HashMap<String, Object> enchant = new HashMap<>( 3 );
            enchant.put( "name", enchantment.getName() );
            enchant.put( "level", level );
            enchant.put( "displayName", enchantment.getDisplayName( level ).getString() );
            enchants.add( enchant );
        } );
        return enchants;
    }
}
