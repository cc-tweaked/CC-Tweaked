/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;

public class DiskRecipe extends SpecialRecipe
{
    private final Ingredient paper = Ingredient.of( Items.PAPER );
    private final Ingredient redstone = Ingredient.of( Tags.Items.DUSTS_REDSTONE );

    public DiskRecipe( ResourceLocation id )
    {
        super( id );
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inv, @Nonnull World world )
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
                    if( paperFound ) return false;
                    paperFound = true;
                }
                else if( redstone.test( stack ) )
                {
                    if( redstoneFound ) return false;
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
    public ItemStack assemble( @Nonnull CraftingInventory inv )
    {
        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.getContainerSize(); i++ )
        {
            ItemStack stack = inv.getItem( i );

            if( stack.isEmpty() ) continue;

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
    public ItemStack getResultItem()
    {
        return ItemDisk.createFromIDAndColour( -1, null, Colour.BLUE.getHex() );
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final IRecipeSerializer<DiskRecipe> SERIALIZER = new SpecialRecipeSerializer<>( DiskRecipe::new );
}
