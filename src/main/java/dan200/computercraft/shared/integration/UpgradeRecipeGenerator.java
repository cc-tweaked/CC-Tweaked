/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static dan200.computercraft.shared.integration.RecipeModHelpers.MAIN_FAMILIES;

/**
 * Provides dynamic recipe and usage information for upgraded turtle and pocket computers. This is intended to be
 * consumed by our recipe mod plugins (for example JEI).
 *
 * @param <T> The type the recipe mod uses for recipes.
 * @see RecipeModHelpers
 */
public class UpgradeRecipeGenerator<T>
{
    private static final ResourceLocation TURTLE_UPGRADE = new ResourceLocation( ComputerCraft.MOD_ID, "turtle_upgrade" );
    private static final ResourceLocation POCKET_UPGRADE = new ResourceLocation( ComputerCraft.MOD_ID, "pocket_upgrade" );

    private final Function<CraftingRecipe, T> wrap;

    private final Map<Item, List<UpgradeInfo>> upgradeItemLookup = new HashMap<>();
    private final List<UpgradeInfo> pocketUpgrades = new ArrayList<>();
    private final List<UpgradeInfo> turtleUpgrades = new ArrayList<>();
    private boolean initialised = false;

    public UpgradeRecipeGenerator( Function<CraftingRecipe, T> wrap )
    {
        this.wrap = wrap;
    }

    /**
     * Build a cache of items which are used for turtle and pocket computer upgrades.
     */
    private void setupCache()
    {
        if( initialised ) return;
        initialised = true;

        for( ITurtleUpgrade upgrade : TurtleUpgrades.instance().getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) return;

            UpgradeInfo info = new UpgradeInfo( stack, upgrade );
            upgradeItemLookup.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( info );
            turtleUpgrades.add( info );
        }

        for( IPocketUpgrade upgrade : PocketUpgrades.instance().getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) return;

            UpgradeInfo info = new UpgradeInfo( stack, upgrade );
            upgradeItemLookup.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( info );
            pocketUpgrades.add( info );
        }
    }

    /**
     * Check if this item is usable as a turtle or pocket computer upgrade.
     *
     * @param stack The stack to check.
     * @return Whether the item is an upgrade.
     */
    public boolean isUpgrade( ItemStack stack )
    {
        if( stack.isEmpty() ) return false;

        setupCache();
        List<UpgradeInfo> upgrades = upgradeItemLookup.get( stack.getItem() );
        if( upgrades == null ) return false;

        for( UpgradeInfo upgrade : upgrades )
        {
            ItemStack craftingStack = upgrade.stack;
            if( !craftingStack.isEmpty() && craftingStack.getItem() == stack.getItem() && upgrade.upgrade.isItemSuitable( stack ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Find all usages of the given stack.
     *
     * @param stack The stack to find usages of.
     * @return All upgrade recipes which take the current stack as an input.
     */
    public List<T> findRecipesWithInput( ItemStack stack )
    {
        setupCache();

        if( stack.getItem() instanceof ItemTurtle item )
        {
            // Suggest possible upgrades which can be applied to this turtle
            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.LEFT );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.RIGHT );
            if( left != null && right != null ) return Collections.emptyList();

            List<T> recipes = new ArrayList<>();
            Ingredient ingredient = Ingredient.of( stack );
            for( UpgradeInfo upgrade : turtleUpgrades )
            {
                // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
                if( left == null )
                {
                    recipes.add( turtle( ingredient, upgrade.ingredient, turtleWith( stack, upgrade.turtle, right ) ) );
                }

                if( right == null )
                {
                    recipes.add( turtle( upgrade.ingredient, ingredient, turtleWith( stack, left, upgrade.turtle ) ) );
                }
            }

            return Collections.unmodifiableList( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            // Suggest possible upgrades which can be applied to this turtle
            IPocketUpgrade back = ItemPocketComputer.getUpgrade( stack );
            if( back != null ) return Collections.emptyList();

            List<T> recipes = new ArrayList<>();
            Ingredient ingredient = Ingredient.of( stack );
            for( UpgradeInfo upgrade : pocketUpgrades )
            {
                recipes.add( pocket( upgrade.ingredient, ingredient, pocketWith( stack, upgrade.pocket ) ) );
            }

            return Collections.unmodifiableList( recipes );
        }
        else
        {
            // If this item is usable as an upgrade, find all possible recipes.
            List<UpgradeInfo> upgrades = upgradeItemLookup.get( stack.getItem() );
            if( upgrades == null ) return Collections.emptyList();

            List<T> recipes = null;
            boolean multiple = false;
            for( UpgradeInfo upgrade : upgrades )
            {
                ItemStack craftingStack = upgrade.stack;
                if( craftingStack.isEmpty() || craftingStack.getItem() != stack.getItem() || !upgrade.upgrade.isItemSuitable( stack ) )
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

            return recipes == null ? Collections.emptyList() : Collections.unmodifiableList( recipes );
        }
    }

    /**
     * Find all recipes for the given stack.
     *
     * @param stack The stack to find recipes of.
     * @return All upgrade recipes which produce the stack as an output.
     */
    public List<T> findRecipesWithOutput( ItemStack stack )
    {
        // Find which upgrade this item currently has, and so how we could build it.
        if( stack.getItem() instanceof ItemTurtle item )
        {
            List<T> recipes = new ArrayList<>( 0 );

            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.LEFT );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.RIGHT );

            // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
            if( left != null )
            {
                recipes.add( turtle(
                    Ingredient.of( turtleWith( stack, null, right ) ),
                    Ingredient.of( left.getCraftingItem() ),
                    stack
                ) );
            }

            if( right != null )
            {
                recipes.add( turtle(
                    Ingredient.of( right.getCraftingItem() ),
                    Ingredient.of( turtleWith( stack, left, null ) ),
                    stack
                ) );
            }

            return Collections.unmodifiableList( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            List<T> recipes = new ArrayList<>( 0 );

            IPocketUpgrade back = ItemPocketComputer.getUpgrade( stack );
            if( back != null )
            {
                recipes.add( pocket( Ingredient.of( back.getCraftingItem() ), Ingredient.of( pocketWith( stack, null ) ), stack ) );
            }

            return Collections.unmodifiableList( recipes );
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private static ItemStack turtleWith( ItemStack stack, @Nullable ITurtleUpgrade left, @Nullable ITurtleUpgrade right )
    {
        ItemTurtle item = (ItemTurtle) stack.getItem();
        return TurtleItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily(),
            left, right, item.getFuelLevel( stack ), item.getOverlay( stack )
        );
    }

    private static ItemStack pocketWith( ItemStack stack, @Nullable IPocketUpgrade back )
    {
        ItemPocketComputer item = (ItemPocketComputer) stack.getItem();
        return PocketComputerItemFactory.create(
            item.getComputerID( stack ), item.getLabel( stack ), item.getColour( stack ), item.getFamily(),
            back
        );
    }

    private T pocket( Ingredient upgrade, Ingredient pocketComputer, ItemStack result )
    {
        return wrap.apply( new ShapedRecipe( POCKET_UPGRADE, "", 1, 2, NonNullList.of( Ingredient.EMPTY, upgrade, pocketComputer ), result ) );
    }

    private T turtle( Ingredient left, Ingredient right, ItemStack result )
    {
        return wrap.apply( new ShapedRecipe( TURTLE_UPGRADE, "", 2, 1, NonNullList.of( Ingredient.EMPTY, left, right ), result ) );
    }

    private class UpgradeInfo
    {
        final ItemStack stack;
        final Ingredient ingredient;
        final @Nullable ITurtleUpgrade turtle;
        final @Nullable IPocketUpgrade pocket;
        final IUpgradeBase upgrade;
        private @Nullable ArrayList<T> recipes;

        UpgradeInfo( ItemStack stack, ITurtleUpgrade turtle )
        {
            this.stack = stack;
            ingredient = Ingredient.of( stack );
            upgrade = this.turtle = turtle;
            pocket = null;
        }

        UpgradeInfo( ItemStack stack, IPocketUpgrade pocket )
        {
            this.stack = stack;
            ingredient = Ingredient.of( stack );
            turtle = null;
            upgrade = this.pocket = pocket;
        }

        List<T> getRecipes()
        {
            ArrayList<T> recipes = this.recipes;
            if( recipes != null ) return recipes;

            recipes = this.recipes = new ArrayList<>( 4 );
            for( ComputerFamily family : MAIN_FAMILIES )
            {
                if( turtle != null )
                {
                    recipes.add( turtle(
                        ingredient, // Right upgrade, recipe on left
                        Ingredient.of( TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null ) ),
                        TurtleItemFactory.create( -1, null, -1, family, null, turtle, 0, null )
                    ) );
                }

                if( pocket != null )
                {
                    recipes.add( pocket(
                        ingredient,
                        Ingredient.of( PocketComputerItemFactory.create( -1, null, -1, family, null ) ),
                        PocketComputerItemFactory.create( -1, null, -1, family, pocket )
                    ) );
                }
            }

            recipes.trimToSize();
            return recipes;
        }
    }
}
