/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public final class ColourableRecipe extends SpecialRecipe
{
    private ColourableRecipe( ResourceLocation id )
    {
        super( id );
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inv, @Nonnull World world )
    {
        boolean hasColourable = false;
        boolean hasDye = false;
        for( int i = 0; i < inv.getContainerSize(); i++ )
        {
            ItemStack stack = inv.getItem( i );
            if( stack.isEmpty() ) continue;

            if( stack.getItem() instanceof IColouredItem )
            {
                if( hasColourable ) return false;
                hasColourable = true;
            }
            else if( ColourUtils.getStackColour( stack ) != null )
            {
                hasDye = true;
            }
            else
            {
                return false;
            }
        }

        return hasColourable && hasDye;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingInventory inv )
    {
        ItemStack colourable = ItemStack.EMPTY;

        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.getContainerSize(); i++ )
        {
            ItemStack stack = inv.getItem( i );

            if( stack.isEmpty() ) continue;

            if( stack.getItem() instanceof IColouredItem )
            {
                colourable = stack;
            }
            else
            {
                DyeColor dye = ColourUtils.getStackColour( stack );
                if( dye != null ) tracker.addColour( dye );
            }
        }

        if( colourable.isEmpty() ) return ItemStack.EMPTY;

        ItemStack stack = ((IColouredItem) colourable.getItem()).withColour( colourable, tracker.getColour() );
        stack.setCount( 1 );
        return stack;
    }

    @Override
    public boolean canCraftInDimensions( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Override
    @Nonnull
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final SpecialRecipeSerializer<?> SERIALIZER = new SpecialRecipeSerializer<>( ColourableRecipe::new );
}
