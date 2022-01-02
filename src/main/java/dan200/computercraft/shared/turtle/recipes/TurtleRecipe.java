/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.recipes;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nonnull;

public final class TurtleRecipe extends ComputerFamilyRecipe
{
    private TurtleRecipe( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
    {
        super( identifier, group, width, height, ingredients, result, family );
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    protected ItemStack convert( @Nonnull IComputerItem item, @Nonnull ItemStack stack )
    {
        int computerID = item.getComputerID( stack );
        String label = item.getLabel( stack );

        return TurtleItemFactory.create( computerID, label, -1, getFamily(), null, null, 0, null );
    }

    public static final RecipeSerializer<TurtleRecipe> SERIALIZER = new Serializer<>()
    {
        @Override
        protected TurtleRecipe create( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
        {
            return new TurtleRecipe( identifier, group, width, height, ingredients, result, family );
        }
    };
}
