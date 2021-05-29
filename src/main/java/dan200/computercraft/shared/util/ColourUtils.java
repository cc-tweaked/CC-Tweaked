/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.Tag;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;

public final class ColourUtils
{
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static final Tag<Item>[] DYES = new Tag[] {
        Tags.Items.DYES_WHITE,
        Tags.Items.DYES_ORANGE,
        Tags.Items.DYES_MAGENTA,
        Tags.Items.DYES_LIGHT_BLUE,
        Tags.Items.DYES_YELLOW,
        Tags.Items.DYES_LIME,
        Tags.Items.DYES_PINK,
        Tags.Items.DYES_GRAY,
        Tags.Items.DYES_LIGHT_GRAY,
        Tags.Items.DYES_CYAN,
        Tags.Items.DYES_PURPLE,
        Tags.Items.DYES_BLUE,
        Tags.Items.DYES_BROWN,
        Tags.Items.DYES_GREEN,
        Tags.Items.DYES_RED,
        Tags.Items.DYES_BLACK,
    };

    @Nullable
    private ColourUtils() {}

    public static DyeColor getStackColour( ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        for( int i = 0; i < DYES.length; i++ )
        {
            Tag<Item> dye = DYES[i];
            if( dye.contains( stack.getItem() ) ) return DyeColor.byId( i );
        }

        return null;
    }
}
