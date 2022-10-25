/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

class RecipeResolver implements IRecipeManagerPlugin
{
    private final UpgradeRecipeGenerator<CraftingRecipe> resolver = new UpgradeRecipeGenerator<>( x -> x );

    @Nonnull
    @Override
    public <V> List<ResourceLocation> getRecipeCategoryUids( IFocus<V> focus )
    {
        V value = focus.getTypedValue().getIngredient();
        if( !(value instanceof ItemStack stack) ) return Collections.emptyList();

        switch( focus.getRole() )
        {
            case INPUT:
                return stack.getItem() instanceof ItemTurtle || stack.getItem() instanceof ItemPocketComputer || resolver.isUpgrade( stack )
                    ? Collections.singletonList( VanillaRecipeCategoryUid.CRAFTING )
                    : Collections.emptyList();
            case OUTPUT:
                return stack.getItem() instanceof ItemTurtle || stack.getItem() instanceof ItemPocketComputer
                    ? Collections.singletonList( VanillaRecipeCategoryUid.CRAFTING )
                    : Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public <T, V> List<T> getRecipes( @Nonnull IRecipeCategory<T> recipeCategory, IFocus<V> focus )
    {
        if( !(focus.getTypedValue().getIngredient() instanceof ItemStack stack) || !recipeCategory.getUid().equals( VanillaRecipeCategoryUid.CRAFTING ) )
        {
            return Collections.emptyList();
        }

        switch( focus.getRole() )
        {
            case INPUT:
                return cast( resolver.findRecipesWithInput( stack ) );
            case OUTPUT:
                return cast( resolver.findRecipesWithOutput( stack ) );
            default:
                return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public <T> List<T> getRecipes( @Nonnull IRecipeCategory<T> recipeCategory )
    {
        return Collections.emptyList();
    }


    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static <T, U> List<T> cast( List<U> from )
    {
        return (List) from;
    }
}
