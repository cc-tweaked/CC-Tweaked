/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import com.google.gson.JsonParseException;
import dan200.computercraft.shared.util.NBTUtil;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.fabricmc.fabric.impl.tag.extension.TagDelegate;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tag.Tag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Data providers for items.
 */
public class ItemData
{
    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasicSafe( @Nonnull T data, @Nonnull ItemStack stack )
    {
        data.put( "name", DataHelpers.getId( stack.getItem() ) );
        data.put( "count", stack.getCount() );

        return data;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasic( @Nonnull T data, @Nonnull ItemStack stack )
    {
        fillBasicSafe( data, stack );
        String hash = NBTUtil.getNBTHash( stack.getTag() );
        if( hash != null ) data.put( "nbt", hash );

        return data;
    }

    @Nonnull
    public static <T extends Map<? super String, Object>> T fill( @Nonnull T data, @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return data;

        fillBasic( data, stack );

        data.put( "displayName", stack.getName().getString() );
        data.put( "maxCount", stack.getMaxCount() );

        if( stack.isDamageable() )
        {
            data.put( "damage", stack.getDamage() );
            data.put( "maxDamage", stack.getMaxDamage() );
        }

        if( stack.isDamaged() )
        {
            data.put( "durability", 1.0 - ( stack.getDamage() / stack.getMaxDamage() ) );
        }

        /*
         * Used to hide some data from ItemStack tooltip.
         * @see https://minecraft.gamepedia.com/Tutorials/Command_NBT_tags
         * @see ItemStack#getTooltip
         */
        CompoundTag tag = stack.getTag();
        int hideFlags = tag != null ? tag.getInt( "HideFlags" ) : 0;

        List<Map<String, Object>> enchants = getAllEnchants( stack, hideFlags );
        if( !enchants.isEmpty() ) data.put( "enchantments", enchants );

        if( tag != null && tag.getBoolean( "Unbreakable" ) && (hideFlags & 4) == 0 )
        {
            data.put( "unbreakable", true );
        }

        // data.put("tags", DataHelpers.getTags( ??? ));

        return data;
    }

    @Nullable
    private static Text parseTextComponent( @Nonnull Tag x )
    {
        try
        {
            return Text.Serializer.fromJson( x.toString() );
        }
        catch( JsonParseException e )
        {
            return null;
        }
    }

    /**
     * Retrieve all visible enchantments from given stack. Try to follow all tooltip rules : order and visibility.
     *
     * @param stack     Stack to analyse
     * @param hideFlags An int used as bit field to provide visibility rules.
     * @return A filled list that contain all visible enchantments.
     */
    @Nonnull
    private static List<Map<String, Object>> getAllEnchants( @Nonnull ItemStack stack, int hideFlags )
    {
        ArrayList<Map<String, Object>> enchants = new ArrayList<>( 0 );

        if( stack.getItem() instanceof EnchantedBookItem && (hideFlags & 32) == 0 )
        {
            addEnchantments( EnchantedBookItem.getEnchantmentTag( stack ), enchants );
        }

        if( stack.hasEnchantments() && (hideFlags & 1) == 0 )
        {
            /*
             * Mimic the EnchantmentHelper.getEnchantments(ItemStack stack) behavior without special case for Enchanted book.
             * I'll do that to have the same data than ones displayed in tooltip.
             * @see EnchantmentHelper.getEnchantments(ItemStack stack)
             */
            addEnchantments( stack.getEnchantments(), enchants );
        }

        return enchants;
    }

    /**
     * Converts a Mojang enchant map to a Lua list.
     *
     * @param rawEnchants The raw NBT list of enchantments
     * @param enchants    The enchantment map to add it to.
     * @see EnchantmentHelper
     */
    private static void addEnchantments( @Nonnull ListTag rawEnchants, @Nonnull ArrayList<Map<String, Object>> enchants )
    {
        if( rawEnchants.isEmpty() ) return;

        enchants.ensureCapacity( enchants.size() + rawEnchants.size() );


        for( Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.fromTag( rawEnchants ).entrySet() )
        {
            Enchantment enchantment = entry.getKey();
            Integer level = entry.getValue();
            HashMap<String, Object> enchant = new HashMap<>( 3 );
            enchant.put( "name", DataHelpers.getId( enchantment ) );
            enchant.put( "level", level );
            enchant.put( "displayName", enchantment.getName( level ).getString() );
            enchants.add( enchant );
        }
    }
}
