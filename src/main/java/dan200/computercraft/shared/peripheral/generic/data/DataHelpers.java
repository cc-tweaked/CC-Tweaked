/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class DataHelpers
{
    private DataHelpers()
    {}

    @Nonnull
    public static Map<String, Boolean> getTags( @Nonnull Collection<ResourceLocation> tags )
    {
        Map<String, Boolean> result = new HashMap<>( tags.size() );
        for( ResourceLocation location : tags ) result.put( location.toString(), true );
        return result;
    }

    @Nullable
    public static String getId( @Nonnull Block block )
    {
        ResourceLocation id = Registry.BLOCK.getKey( block );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Item item )
    {
        ResourceLocation id = Registry.ITEM.getKey( item );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Enchantment enchantment )
    {
        ResourceLocation id = Registry.ENCHANTMENT.getKey( enchantment );
        return id == null ? null : id.toString();
    }
}
