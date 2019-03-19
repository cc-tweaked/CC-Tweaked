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
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;

import static java.util.Arrays.asList;

class RecipeResolver implements IRecipeRegistryPlugin
{
    static final ComputerFamily[] MAIN_FAMILIES = new ComputerFamily[] { ComputerFamily.Normal, ComputerFamily.Advanced };

    private final Map<Item, List<UpgradeInfo>> upgradeItemLookup = new HashMap<>();
    private final List<UpgradeInfo> pocketUpgrades = new ArrayList<>();
    private final List<UpgradeInfo> turtleUpgrades = new ArrayList<>();
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

            UpgradeInfo info = new UpgradeInfo( stack, upgrade );
            upgradeItemLookup.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( info );
            turtleUpgrades.add( info );
        }

        for( IPocketUpgrade upgrade : PocketUpgrades.getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) continue;

            UpgradeInfo info = new UpgradeInfo( stack, upgrade );
            upgradeItemLookup.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( info );
            pocketUpgrades.add( info );
        }
    }

    private boolean hasUpgrade( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return false;

        setupCache();
        List<UpgradeInfo> upgrades = upgradeItemLookup.get( stack.getItem() );
        if( upgrades == null ) return false;

        for( UpgradeInfo upgrade : upgrades )
        {
            ItemStack craftingStack = upgrade.stack;
            if( !craftingStack.isEmpty() && InventoryUtil.areItemsSimilar( stack, craftingStack ) ) return true;
        }

        return false;
    }

    @Nonnull
    @Override
    public <V> List<String> getRecipeCategoryUids( @Nonnull IFocus<V> focus )
    {
        V value = focus.getValue();
        if( !(value instanceof ItemStack) ) return Collections.emptyList();

        ItemStack stack = (ItemStack) value;
        switch( focus.getMode() )
        {
            case INPUT:
                return stack.getItem() instanceof ITurtleItem || stack.getItem() instanceof ItemPocketComputer ||
                    hasUpgrade( stack )
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
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers( @Nonnull IRecipeCategory<T> recipeCategory, @Nonnull IFocus<V> focus )
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
    public <T extends IRecipeWrapper> List<T> getRecipeWrappers( @Nonnull IRecipeCategory<T> recipeCategory )
    {
        return Collections.emptyList();
    }

    @Nonnull
    private List<Shaped> findRecipesWithInput( @Nonnull ItemStack stack )
    {
        setupCache();

        if( stack.getItem() instanceof ITurtleItem )
        {
            // Suggest possible upgrades which can be applied to this turtle
            ITurtleItem item = (ITurtleItem) stack.getItem();
            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.Left );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.Right );
            if( left != null && right != null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>();
            for( UpgradeInfo upgrade : turtleUpgrades )
            {
                // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
                if( left == null )
                {
                    recipes.add( horizontal( asList( stack, upgrade.stack ), turtleWith( stack, upgrade.turtle, right ) ) );
                }

                if( right == null )
                {
                    recipes.add( horizontal( asList( upgrade.stack, stack ), turtleWith( stack, left, upgrade.turtle ) ) );
                }
            }

            return cast( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            // Suggest possible upgrades which can be applied to this turtle
            ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
            IPocketUpgrade back = item.getUpgrade( stack );
            if( back != null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>();
            for( UpgradeInfo upgrade : pocketUpgrades )
            {
                recipes.add( vertical( asList( stack, upgrade.stack ), pocketWith( stack, upgrade.pocket ) ) );
            }

            return recipes;
        }
        else
        {
            List<UpgradeInfo> upgrades = upgradeItemLookup.get( stack.getItem() );
            if( upgrades == null ) return Collections.emptyList();

            List<Shaped> recipes = null;
            boolean multiple = false;
            for( UpgradeInfo upgrade : upgrades )
            {
                ItemStack craftingStack = upgrade.stack;
                if( craftingStack.isEmpty() || !InventoryUtil.areItemsSimilar( stack, craftingStack ) )
                {
                    continue;
                }

                if( recipes == null )
                {
                    recipes = upgrade.getRecipes();
                }
                else
                {
                    if( !multiple )
                    {
                        multiple = true;
                        recipes = new ArrayList<>( recipes );
                    }
                    recipes.addAll( upgrade.getRecipes() );
                }
            }

            return recipes == null ? Collections.emptyList() : recipes;
        }
    }

    @Nonnull
    private List<Shaped> findRecipesWithOutput( @Nonnull ItemStack stack )
    {
        // Find which upgrade this item currently has, an so how we could build it.
        if( stack.getItem() instanceof ITurtleItem )
        {
            ITurtleItem item = (ITurtleItem) stack.getItem();
            List<IRecipeWrapper> recipes = new ArrayList<>( 0 );

            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.Left );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.Right );

            // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
            if( left != null )
            {
                recipes.add( horizontal( asList( turtleWith( stack, null, right ), left.getCraftingItem() ), stack ) );
            }

            if( right != null )
            {
                recipes.add( horizontal( asList( right.getCraftingItem(), turtleWith( stack, left, null ) ), stack ) );
            }

            return cast( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
            List<IRecipeWrapper> recipes = new ArrayList<>( 0 );

            IPocketUpgrade back = item.getUpgrade( stack );
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
    private static <T extends IRecipeWrapper, U extends IRecipeWrapper> List<T> cast( List<U> from )
    {
        return (List) from;
    }

    private static ItemStack turtleWith( ItemStack stack, ITurtleUpgrade left, ITurtleUpgrade right )
    {
        ITurtleItem item = (ITurtleItem) stack.getItem();
        return TurtleItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily( stack ),
            left, right, item.getFuelLevel( stack ), item.getOverlay( stack )
        );
    }

    private static ItemStack pocketWith( ItemStack stack, IPocketUpgrade back )
    {
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        return PocketComputerItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily( stack ),
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

    private static class Shaped implements IShapedCraftingRecipeWrapper
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
        public void getIngredients( @Nonnull IIngredients ingredients )
        {
            ingredients.setInputs( VanillaTypes.ITEM, input );
            ingredients.setOutput( VanillaTypes.ITEM, output );
        }
    }

    private static class UpgradeInfo
    {
        final ItemStack stack;
        final ITurtleUpgrade turtle;
        final IPocketUpgrade pocket;
        ArrayList<Shaped> recipes;

        UpgradeInfo( ItemStack stack, ITurtleUpgrade turtle )
        {
            this.stack = stack;
            this.turtle = turtle;
            this.pocket = null;
        }

        UpgradeInfo( ItemStack stack, IPocketUpgrade pocket )
        {
            this.stack = stack;
            this.turtle = null;
            this.pocket = pocket;
        }

        List<Shaped> getRecipes()
        {
            ArrayList<Shaped> recipes = this.recipes;
            if( recipes != null ) return recipes;

            recipes = this.recipes = new ArrayList<>( 4 );
            for( ComputerFamily family : MAIN_FAMILIES )
            {
                if( turtle != null && TurtleUpgrades.suitableForFamily( family, turtle ) )
                {
                    recipes.add( horizontal(
                        asList( stack, TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null ) ),
                        TurtleItemFactory.create( -1, null, -1, family, null, turtle, 0, null )
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

            recipes.trimToSize();
            return recipes;
        }
    }
}
