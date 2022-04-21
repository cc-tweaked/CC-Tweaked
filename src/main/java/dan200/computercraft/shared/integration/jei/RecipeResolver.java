/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.upgrades.IUpgradeBase;
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
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;
import java.util.*;

import static net.minecraft.core.NonNullList.of;
import static net.minecraft.world.item.crafting.Ingredient.of;

class RecipeResolver implements IRecipeManagerPlugin
{
    static final ComputerFamily[] MAIN_FAMILIES = new ComputerFamily[] { ComputerFamily.NORMAL, ComputerFamily.ADVANCED };

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

        for( ITurtleUpgrade upgrade : TurtleUpgrades.instance().getUpgrades() )
        {
            ItemStack stack = upgrade.getCraftingItem();
            if( stack.isEmpty() ) return;

            UpgradeInfo info = new UpgradeInfo( stack, upgrade );
            upgradeItemLookup.computeIfAbsent( stack.getItem(), k -> new ArrayList<>( 1 ) ).add( info );
            turtleUpgrades.add( info );
        };

        for( IPocketUpgrade upgrade : PocketUpgrades.instance().getUpgrades() )
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
            if( !craftingStack.isEmpty() && craftingStack.getItem() == stack.getItem() && upgrade.upgrade.isItemSuitable( stack ) )
            {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    @Override
    public <V> List<ResourceLocation> getRecipeCategoryUids( @Nonnull IFocus<V> focus )
    {
        V value = focus.getTypedValue().getIngredient();
        if( !(value instanceof ItemStack stack) ) return Collections.emptyList();

        switch( focus.getRole() )
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
    public <T, V> List<T> getRecipes( @Nonnull IRecipeCategory<T> recipeCategory, @Nonnull IFocus<V> focus )
    {
        if( !(focus.getTypedValue().getIngredient() instanceof ItemStack stack) || !recipeCategory.getUid().equals( VanillaRecipeCategoryUid.CRAFTING ) )
        {
            return Collections.emptyList();
        }

        switch( focus.getRole() )
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
        setupCache();

        if( stack.getItem() instanceof ITurtleItem item )
        {
            // Suggest possible upgrades which can be applied to this turtle
            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.LEFT );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.RIGHT );
            if( left != null && right != null ) return Collections.emptyList();

            List<Shaped> recipes = new ArrayList<>();
            Ingredient ingredient = of( stack );
            for( UpgradeInfo upgrade : turtleUpgrades )
            {
                // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
                if( left == null )
                {
                    recipes.add( horizontal( of( Ingredient.EMPTY, ingredient, upgrade.ingredient ), turtleWith( stack, upgrade.turtle, right ) ) );
                }

                if( right == null )
                {
                    recipes.add( horizontal( of( Ingredient.EMPTY, upgrade.ingredient, ingredient ), turtleWith( stack, left, upgrade.turtle ) ) );
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
            Ingredient ingredient = of( stack );
            for( UpgradeInfo upgrade : pocketUpgrades )
            {
                recipes.add( vertical( of( Ingredient.EMPTY, ingredient, upgrade.ingredient ), pocketWith( stack, upgrade.pocket ) ) );
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

            return recipes == null ? Collections.emptyList() : recipes;
        }
    }

    @Nonnull
    private static List<Shaped> findRecipesWithOutput( @Nonnull ItemStack stack )
    {
        // Find which upgrade this item currently has, an so how we could build it.
        if( stack.getItem() instanceof ITurtleItem item )
        {
            List<Shaped> recipes = new ArrayList<>( 0 );

            ITurtleUpgrade left = item.getUpgrade( stack, TurtleSide.LEFT );
            ITurtleUpgrade right = item.getUpgrade( stack, TurtleSide.RIGHT );

            // The turtle is facing towards us, so upgrades on the left are actually crafted on the right.
            if( left != null )
            {
                recipes.add( horizontal(
                    of( Ingredient.EMPTY, of( turtleWith( stack, null, right ) ), of( left.getCraftingItem() ) ),
                    stack
                ) );
            }

            if( right != null )
            {
                recipes.add( horizontal(
                    of( Ingredient.EMPTY, of( right.getCraftingItem() ), of( turtleWith( stack, left, null ) ) ),
                    stack
                ) );
            }

            return cast( recipes );
        }
        else if( stack.getItem() instanceof ItemPocketComputer )
        {
            List<Shaped> recipes = new ArrayList<>( 0 );

            IPocketUpgrade back = ItemPocketComputer.getUpgrade( stack );
            if( back != null )
            {
                recipes.add( vertical(
                    of( Ingredient.EMPTY, of( back.getCraftingItem() ), of( pocketWith( stack, null ) ) ),
                    stack
                ) );
            }

            return cast( recipes );
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
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

    private static Shaped vertical( NonNullList<Ingredient> input, ItemStack result )
    {
        return new Shaped( 1, input.size(), input, result );
    }

    private static Shaped horizontal( NonNullList<Ingredient> input, ItemStack result )
    {
        return new Shaped( input.size(), 1, input, result );
    }

    private static class Shaped extends ShapedRecipe
    {
        private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "impostor" );

        Shaped( int width, int height, NonNullList<Ingredient> input, ItemStack output )
        {
            super( ID, null, width, height, input, output );
        }

        @Nonnull
        @Override
        public RecipeSerializer<?> getSerializer()
        {
            throw new IllegalStateException( "Should not serialise the JEI recipe" );
        }
    }

    private static class UpgradeInfo
    {
        final ItemStack stack;
        final Ingredient ingredient;
        final ITurtleUpgrade turtle;
        final IPocketUpgrade pocket;
        final IUpgradeBase upgrade;
        ArrayList<Shaped> recipes;

        UpgradeInfo( ItemStack stack, ITurtleUpgrade turtle )
        {
            this.stack = stack;
            ingredient = of( stack );
            upgrade = this.turtle = turtle;
            pocket = null;
        }

        UpgradeInfo( ItemStack stack, IPocketUpgrade pocket )
        {
            this.stack = stack;
            ingredient = of( stack );
            turtle = null;
            upgrade = this.pocket = pocket;
        }

        List<Shaped> getRecipes()
        {
            ArrayList<Shaped> recipes = this.recipes;
            if( recipes != null ) return recipes;

            recipes = this.recipes = new ArrayList<>( 4 );
            for( ComputerFamily family : MAIN_FAMILIES )
            {
                if( turtle != null )
                {
                    recipes.add( horizontal(
                        of( Ingredient.EMPTY, ingredient, of( TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null ) ) ),
                        TurtleItemFactory.create( -1, null, -1, family, null, turtle, 0, null )
                    ) );
                }

                if( pocket != null )
                {
                    recipes.add( vertical(
                        of( Ingredient.EMPTY, ingredient, of( PocketComputerItemFactory.create( -1, null, -1, family, null ) ) ),
                        PocketComputerItemFactory.create( -1, null, -1, family, pocket )
                    ) );
                }
            }

            recipes.trimToSize();
            return recipes;
        }
    }
}
