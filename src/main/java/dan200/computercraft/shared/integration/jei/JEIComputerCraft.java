/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.integration.RecipeModHelpers;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IRecipeLookup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@JeiPlugin
public class JEIComputerCraft implements IModPlugin
{
    @Nonnull
    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation( ComputerCraft.MOD_ID, "jei" );
    }

    @Override
    public void registerItemSubtypes( ISubtypeRegistration subtypeRegistry )
    {
        subtypeRegistry.registerSubtypeInterpreter( VanillaTypes.ITEM_STACK, Registry.ModItems.TURTLE_NORMAL.get(), turtleSubtype );
        subtypeRegistry.registerSubtypeInterpreter( VanillaTypes.ITEM_STACK, Registry.ModItems.TURTLE_ADVANCED.get(), turtleSubtype );

        subtypeRegistry.registerSubtypeInterpreter( VanillaTypes.ITEM_STACK, Registry.ModItems.POCKET_COMPUTER_NORMAL.get(), pocketSubtype );
        subtypeRegistry.registerSubtypeInterpreter( VanillaTypes.ITEM_STACK, Registry.ModItems.POCKET_COMPUTER_ADVANCED.get(), pocketSubtype );

        subtypeRegistry.registerSubtypeInterpreter( VanillaTypes.ITEM_STACK, Registry.ModItems.DISK.get(), diskSubtype );
    }

    @Override
    public void registerAdvanced( IAdvancedRegistration registry )
    {
        registry.addRecipeManagerPlugin( new RecipeResolver() );
    }

    @Override
    public void onRuntimeAvailable( IJeiRuntime runtime )
    {
        IRecipeManager registry = runtime.getRecipeManager();

        // Register all turtles/pocket computers (not just vanilla upgrades) as upgrades on JEI.
        List<ItemStack> upgradeItems = RecipeModHelpers.getExtraStacks();
        if( !upgradeItems.isEmpty() )
        {
            runtime.getIngredientManager().addIngredientsAtRuntime( VanillaTypes.ITEM_STACK, upgradeItems );
        }

        // Hide all upgrade recipes
        IRecipeLookup<CraftingRecipe> category = registry.createRecipeLookup( RecipeTypes.CRAFTING );
        category.get().forEach( wrapper -> {
            if( RecipeModHelpers.shouldRemoveRecipe( wrapper.getId() ) )
            {
                registry.hideRecipes( RecipeTypes.CRAFTING, Collections.singleton( wrapper ) );
            }
        } );
    }

    /**
     * Distinguishes turtles by upgrades and family.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> turtleSubtype = ( stack, ctx ) -> {
        Item item = stack.getItem();
        if( !(item instanceof ITurtleItem turtle) ) return IIngredientSubtypeInterpreter.NONE;

        StringBuilder name = new StringBuilder( "turtle:" );

        // Add left and right upgrades to the identifier
        ITurtleUpgrade left = turtle.getUpgrade( stack, TurtleSide.LEFT );
        ITurtleUpgrade right = turtle.getUpgrade( stack, TurtleSide.RIGHT );
        if( left != null ) name.append( left.getUpgradeID() );
        if( left != null && right != null ) name.append( '|' );
        if( right != null ) name.append( right.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes pocket computers by upgrade and family.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> pocketSubtype = ( stack, ctx ) -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemPocketComputer) ) return IIngredientSubtypeInterpreter.NONE;

        StringBuilder name = new StringBuilder( "pocket:" );

        // Add the upgrade to the identifier
        IPocketUpgrade upgrade = ItemPocketComputer.getUpgrade( stack );
        if( upgrade != null ) name.append( upgrade.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes disks by colour.
     */
    private static final IIngredientSubtypeInterpreter<ItemStack> diskSubtype = ( stack, ctx ) -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemDisk disk) ) return IIngredientSubtypeInterpreter.NONE;

        int colour = disk.getColour( stack );
        return colour == -1 ? IIngredientSubtypeInterpreter.NONE : String.format( "%06x", colour );
    };
}
