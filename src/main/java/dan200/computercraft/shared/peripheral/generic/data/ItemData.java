/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import com.google.gson.JsonParseException;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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

        data.put( "displayName", stack.getHoverName().getString() );
        data.put( "maxCount", stack.getMaxStackSize() );

        if( stack.isDamageableItem() )
        {
            data.put( "damage", stack.getDamageValue() );
            data.put( "maxDamage", stack.getMaxDamage() );
        }

        if( stack.getItem().showDurabilityBar( stack ) )
        {
            data.put( "durability", stack.getItem().getDurabilityForDisplay( stack ) );
        }

        data.put( "tags", DataHelpers.getTags( stack.getItem().getTags() ) );
        data.put( "itemGroups", getItemGroups( stack ) );

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
         * @see ItemStack#getTooltip
         */
        int hideFlags = tag != null ? tag.getInt( "HideFlags" ) : 0;

        List<Map<String, Object>> enchants = getAllEnchants( stack, hideFlags );
        if( !enchants.isEmpty() ) data.put( "enchantments", enchants );

        if( tag != null && tag.getBoolean( "Unbreakable" ) && (hideFlags & 4) == 0 )
        {
            data.put( "unbreakable", true );
        }

        DetailProviders.fillData( ItemStack.class, data, stack );

        return data;
    }

    @Nullable
    private static ITextComponent parseTextComponent( @Nonnull INBT x )
    {
        try
        {
            return ITextComponent.Serializer.fromJson( x.getAsString() );
        }
        catch( JsonParseException e )
        {
            return null;
        }
    }

    /**
     * Retrieve all item groups an item stack pertains to.
     *
     * @param stack Stack to analyse
     * @return A filled list that contains pairs of item group IDs and their display names.
     */
    @Nonnull
    private static List<Map<String, Object>> getItemGroups( @Nonnull ItemStack stack )
    {
        List<Map<String, Object>> groups = new ArrayList<>( 1 );

        for( ItemGroup group : stack.getItem().getCreativeTabs() )
        {
            if( group == null ) continue;

            Map<String, Object> groupData = new HashMap<>( 2 );
            groupData.put( "id", group.langId );
            groupData.put( "displayName", group.displayName.getString() );
            groups.add( groupData );
        }

        return groups;
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
            addEnchantments( EnchantedBookItem.getEnchantments( stack ), enchants );
        }

        if( stack.isEnchanted() && (hideFlags & 1) == 0 )
        {
            /*
             * Mimic the EnchantmentHelper.getEnchantments(ItemStack stack) behavior without special case for Enchanted book.
             * I'll do that to have the same data than ones displayed in tooltip.
             * @see EnchantmentHelper.getEnchantments(ItemStack stack)
             */
            addEnchantments( stack.getEnchantmentTags(), enchants );
        }

        return enchants;
    }

    /**
     * Converts a Mojang enchant map to a Lua list.
     *
     * @param rawEnchants The raw NBT list of enchantments
     * @param enchants    The enchantment map to add it to.
     * @see net.minecraft.enchantment.EnchantmentHelper
     */
    private static void addEnchantments( @Nonnull ListNBT rawEnchants, @Nonnull ArrayList<Map<String, Object>> enchants )
    {
        if( rawEnchants.isEmpty() ) return;

        enchants.ensureCapacity( enchants.size() + rawEnchants.size() );

        for( Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.deserializeEnchantments( rawEnchants ).entrySet() )
        {
            Enchantment enchantment = entry.getKey();
            Integer level = entry.getValue();
            HashMap<String, Object> enchant = new HashMap<>( 3 );
            enchant.put( "name", DataHelpers.getId( enchantment ) );
            enchant.put( "level", level );
            enchant.put( "displayName", enchantment.getFullname( level ).getString() );
            enchants.add( enchant );
        }
    }
}
