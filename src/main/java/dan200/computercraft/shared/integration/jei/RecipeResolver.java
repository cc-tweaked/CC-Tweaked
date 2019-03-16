/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.InventoryUtil;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.IShapedCraftingCategoryExtension;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Arrays.asList;

class RecipeResolver implements IRecipeManagerPlugin
{
    static final ComputerFamily[] MAIN_FAMILIES = new ComputerFamily[] { ComputerFamily.Normal, ComputerFamily.Advanced };

    private final Map<Item, List<ITurtleUpgrade>> turtleUpgrades = new HashMap<>();
    private final Map<Item, List<IPocketUpgrade>> pocketUpgrades = new HashMap<>();
    private boolean initialised = false;

    /**
     * Build a cache of items which are used for turtle and pocket computer upgrades.
     */
    private void setupCache()
    {
        if( initialised ) return;
        initialised = true;

        for( ITurtleUpgrade upgrade : TurtleUpgrades.getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) continue;
            turtleUpgrades.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( upgrade );
        }

        for( IPocketUpgrade upgrade : PocketUpgrades.getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) continue;
            pocketUpgrades.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( upgrade );
        }
    }

    @Nullable
    private ITurtleUpgrade getTurtleUpgrade( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        setupCache();
        List<ITurtleUpgrade> upgrades = turtleUpgrades.get( stack.getItem() );
        if( upgrades == null ) return null;

        for( ITurtleUpgrade upgrade : upgrades )
        {
            ItemStack craftingStack = upgrade.getCraftingItem();
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsSimilar( stack, craftingStack ) ) return upgrade;
        }

        return null;
    }

    @Nullable
    private IPocketUpgrade getPocketUpgrade( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        setupCache();
        List<IPocketUpgrade> upgrades = pocketUpgrades.get( stack.getItem() );
        if( upgrades == null ) return null;

        for( IPocketUpgrade upgrade : upgrades )
        {
            ItemStack craftingStack = upgrade.getCraftingItem();
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsSimilar( stack, craftingStack ) ) return upgrade;
        }

        return null;
    }

    @Nonnull
    @Override
    public <V> List<ResourceLocation> getRecipeCategoryUids( @Nonnull IFocus<V> focus )
    {
        V value = focus.getValue();
        if( !(value instanceof ItemStack) ) return Collections.emptyList();

        ItemStack stack = (ItemStack) value;
        switch( focus.getMode() )
        {
            case INPUT:
                return stack.getItem() instanceof ITurtleItem || stack.getItem() instanceof ItemPocketComputer ||
                    getTurtleUpgrade( stack ) != null || getPocketUpgrade( stack ) != null
                    ? Collections.singletonList( VanillaRecipeCategoryUid.CRAFTING )
                    : Collections.emptyList();
            case OUTPUT:
                return stack.getItem() instanceof ITurtleItem || stack.getItem() instanceof ItemPocketComputer
                    ? Collections.singletonList( VanillaRecipeCategoryUid.CRAFTING )
                    : Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public <T, V> List<T> getRecipes( @Nonnull IRecipeCategory<T> recipeCategory, @Nonnull IFocus<V> focus )
    {
        if( !(focus.getValue() instanceof ItemStack) || !recipeCategory.getUid().equals( VanillaRecipeCategoryUid.CRAFTING ) )
        {
            return Collections.emptyList();
        }

        ItemStack stack = (ItemStack) focus.getValue();
        switch( focus.getMode() )
        {
            case INPUT:
                return cast( findRecipesWithInput( stack ) );
            case OUTPUT:
                return cast( findRecipesWithOutput( stack ) );
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

    @Nonnull
    private List<Shaped> findRecipesWithInput( @Nonnull ItemStack stack )
    {
        if( stack.getItem() instanceof ITurtleItem )
        {
            // Suggest possible upgrades which can be applied to this turtle
            ITurtleItem item = (ITurtleItem) stack.getItem();
            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.Left );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.Right );
            if( left != null && right != null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>();
            for( ITurtleUpgrade upgrade : TurtleUpgrades.getUpgrades() )
            {
                if( left == null )
                {
                    recipes.add( horizontal( asList( stack, upgrade.getCraftingItem() ), turtleWith( stack, upgrade, right ) ) );
                }

                if( right == null )
                {
                    recipes.add( horizontal( asList( stack, upgrade.getCraftingItem() ), turtleWith( stack, left, upgrade ) ) );
                }
            }

            return cast( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            // Suggest possible upgrades which can be applied to this turtle
            IPocketUpgrade back = ItemPocketComputer.getUpgrade( stack );
            if( back != null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>();
            for( IPocketUpgrade upgrade : PocketUpgrades.getUpgrades() )
            {
                recipes.add( vertical( asList( stack, upgrade.getCraftingItem() ), pocketWith( stack, upgrade ) ) );
            }

            return recipes;
        }
        else
        {
            // Find places this may be used as an upgrade.
            ITurtleUpgrade turtle = getTurtleUpgrade( stack );
            IPocketUpgrade pocket = getPocketUpgrade( stack );
            if( turtle == null && pocket == null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>( 1 );
            for( ComputerFamily family : MAIN_FAMILIES )
            {
                if( turtle != null && TurtleUpgrades.suitableForFamily( family, turtle ) )
                {
                    recipes.add( horizontal(
                        asList( stack, TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null ) ),
                        TurtleItemFactory.create( -1, null, -1, family, turtle, null, 0, null )
                    ) );
                }

                if( pocket != null )
                {
                    recipes.add( vertical(
                        asList( stack, PocketComputerItemFactory.create( -1, null, -1, family, null ) ),
                        PocketComputerItemFactory.create( -1, null, -1, family, pocket )
                    ) );
                }
            }
            return recipes;
        }
    }

    @Nonnull
    private List<Shaped> findRecipesWithOutput( @Nonnull ItemStack stack )
    {
        // Find which upgrade this item currently has, an so how we could build it.
        if( stack.getItem() instanceof ITurtleItem )
        {
            ITurtleItem item = (ITurtleItem) stack.getItem();
            List<Shaped> recipes = new ArrayList<>( 0 );

            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.Left );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.Right );

            if( left != null )
            {
                recipes.add( horizontal( asList( left.getCraftingItem(), turtleWith( stack, null, right ) ), stack ) );
            }

            if( right != null )
            {
                recipes.add( horizontal( asList( turtleWith( stack, left, null ), right.getCraftingItem() ), stack ) );
            }

            return cast( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            List<Shaped> recipes = new ArrayList<>( 0 );

            IPocketUpgrade back = ItemPocketComputer.getUpgrade( stack );
            if( back != null )
            {
                recipes.add( vertical( asList( back.getCraftingItem(), pocketWith( stack, null ) ), stack ) );
            }

            return cast( recipes );
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings( "unchecked" )
    private static <T, U> List<T> cast( List<U> from )
    {
        return (List) from;
    }

    private static ItemStack turtleWith( ItemStack stack, ITurtleUpgrade left, ITurtleUpgrade right )
    {
        ITurtleItem item = (ITurtleItem) stack.getItem();
        return TurtleItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily(),
            left, right, item.getFuelLevel( stack ), item.getOverlay( stack )
        );
    }

    private static ItemStack pocketWith( ItemStack stack, IPocketUpgrade back )
    {
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        return PocketComputerItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily(),
            back
        );
    }

    private static Shaped vertical( List<ItemStack> input, ItemStack result )
    {
        return new Shaped( 1, input.size(), input, result );
    }

    private static Shaped horizontal( List<ItemStack> input, ItemStack result )
    {
        return new Shaped( input.size(), 1, input, result );
    }

    static class Shaped implements IShapedCraftingCategoryExtension
    {
        private final int width;
        private final int height;
        private final List<ItemStack> input;
        private final ItemStack output;

        Shaped( int width, int height, List<ItemStack> input, ItemStack output )
        {
            this.width = width;
            this.height = height;
            this.input = input;
            this.output = output;
        }

        @Override
        public int getWidth()
        {
            return width;
        }

        @Override
        public int getHeight()
        {
            return height;
        }

        @Override
        public void setIngredients( @Nonnull IIngredients ingredients )
        {
            ingredients.setInputs( VanillaTypes.ITEM, input );
            ingredients.setOutput( VanillaTypes.ITEM, output );
        }
    }
}
