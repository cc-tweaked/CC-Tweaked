/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.item.EnumDyeColor;
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

    public static EnumDyeColor getStackColour( ItemStack stack )
    {
        for( int i = 0; i < DYES.length; i++ )
        {
            Tag<Item> dye = DYES[i];
            if( dye.contains( stack.getItem() ) ) return EnumDyeColor.byId( i );
        }

        return null;
    }
}
