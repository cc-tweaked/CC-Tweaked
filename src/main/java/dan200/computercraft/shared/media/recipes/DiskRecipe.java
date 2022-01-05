/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class DiskRecipe extends CustomRecipe
{
    public static final RecipeSerializer<DiskRecipe> SERIALIZER = new SimpleRecipeSerializer<>( DiskRecipe::new );
    private final Ingredient paper = Ingredient.of( Items.PAPER );
    // TODO: Ingredient.fromTag( Tags.Items.DUSTS_REDSTONE );
    private final Ingredient redstone = Ingredient.of( Items.REDSTONE );

    public DiskRecipe( ResourceLocation id )
    {
        super( id );
    }

    @Override
    public boolean matches( @Nonnull CraftingContainer inv, @Nonnull Level world )
    {
        boolean paperFound = false;
        boolean redstoneFound = false;

        for( int i = 0; i < inv.getContainerSize(); i++ )
        {
            ItemStack stack = inv.getItem( i );

            if( !stack.isEmpty() )
            {
                if( paper.test( stack ) )
                {
                    if( paperFound )
                    {
                        return false;
                    }
                    paperFound = true;
                }
                else if( redstone.test( stack ) )
                {
                    if( redstoneFound )
                    {
                        return false;
                    }
                    redstoneFound = true;
                }
                else if( ColourUtils.getStackColour( stack ) == null )
                {
                    return false;
                }
            }
        }

        return redstoneFound && paperFound;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingContainer inv )
    {
        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.getContainerSize(); i++ )
        {
            ItemStack stack = inv.getItem( i );

            if( stack.isEmpty() )
            {
                continue;
            }

            if( !paper.test( stack ) && !redstone.test( stack ) )
            {
                DyeColor dye = ColourUtils.getStackColour( stack );
                if( dye != null ) tracker.addColour( dye );
            }
        }

        return ItemDisk.createFromIDAndColour( -1, null, tracker.hasColour() ? tracker.getColour() : Colour.BLUE.getHex() );
    }

    @Override
    public boolean canCraftInDimensions( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem()
    {
        return ItemDisk.createFromIDAndColour( -1, null, Colour.BLUE.getHex() );
    }
}
