/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import mezz.jei.api.*;
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.integration.jei.RecipeResolver.MAIN_FAMILIES;

@JEIPlugin
public class JEIComputerCraft implements IModPlugin
{
    private IIngredientRegistry ingredients;

    @Override
    public void registerItemSubtypes( ISubtypeRegistry subtypeRegistry )
    {
        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.turtle, turtleSubtype );
        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.turtleExpanded, turtleSubtype );
        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.turtleAdvanced, turtleSubtype );

        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.pocketComputer, pocketSubtype );

        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.disk, diskSubtype );
        subtypeRegistry.registerSubtypeInterpreter( ComputerCraft.Items.diskExpanded, diskSubtype );
    }

    @Override
    public void register( IModRegistry registry )
    {
        ingredients = registry.getIngredientRegistry();

        // Hide treasure disks from the ingredient list
        registry.getJeiHelpers().getIngredientBlacklist()
            .addIngredientToBlacklist( new ItemStack( ComputerCraft.Items.treasureDisk, OreDictionary.WILDCARD_VALUE ) );

        registry.addRecipeRegistryPlugin( new RecipeResolver() );
    }

    @Override
    public void onRuntimeAvailable( IJeiRuntime runtime )
    {
        IRecipeRegistry registry = runtime.getRecipeRegistry();

        // Register all turtles/pocket computers (not just vanilla upgrades) as upgrades on JEI.
        List<ItemStack> upgradeItems = new ArrayList<>();
        for( ComputerFamily family : MAIN_FAMILIES )
        {
            for( ITurtleUpgrade upgrade : TurtleUpgrades.getUpgrades() )
            {
                if( !TurtleUpgrades.suitableForFamily( family, upgrade ) ) continue;

                upgradeItems.add( TurtleItemFactory.create( -1, null, -1, family, null, upgrade, 0, null ) );
            }

            for( IPocketUpgrade upgrade : PocketUpgrades.getUpgrades() )
            {
                upgradeItems.add( PocketComputerItemFactory.create( -1, null, -1, family, upgrade ) );
            }
        }
        ingredients.addIngredientsAtRuntime( VanillaTypes.ITEM, upgradeItems );

        // Hide all upgrade recipes
        IRecipeCategory<? extends IRecipeWrapper> category = (IRecipeCategory<? extends IRecipeWrapper>) registry.getRecipeCategory( VanillaRecipeCategoryUid.CRAFTING );
        if( category != null )
        {
            for( IRecipeWrapper wrapper : registry.getRecipeWrappers( category ) )
            {
                if( !(wrapper instanceof ICraftingRecipeWrapper) ) continue;
                ResourceLocation id = ((ICraftingRecipeWrapper) wrapper).getRegistryName();
                if( id != null && id.getNamespace().equals( ComputerCraft.MOD_ID )
                    && (id.getPath().startsWith( "generated/turtle_" ) || id.getPath().startsWith( "generated/pocket_" )) )
                {
                    registry.hideRecipe( wrapper, VanillaRecipeCategoryUid.CRAFTING );
                }
            }
        }
    }

    /**
     * Distinguishes turtles by upgrades and family
     */
    private static final ISubtypeInterpreter turtleSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ITurtleItem) ) return "";

        ITurtleItem turtle = (ITurtleItem) item;
        StringBuilder name = new StringBuilder();

        name.append( turtle.getFamily( stack ).toString() );

        // Add left and right upgrades to the identifier
        ITurtleUpgrade left = turtle.getUpgrade( stack, TurtleSide.Left );
        name.append( '|' );
        if( left != null ) name.append( left.getUpgradeID() );

        ITurtleUpgrade right = turtle.getUpgrade( stack, TurtleSide.Right );
        name.append( '|' );
        if( right != null ) name.append( '|' ).append( right.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes pocket computers by upgrade and family
     */
    private static final ISubtypeInterpreter pocketSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemPocketComputer) ) return "";

        ItemPocketComputer pocket = (ItemPocketComputer) item;
        StringBuilder name = new StringBuilder();

        name.append( pocket.getFamily( stack ).toString() );

        // Add the upgrade to the identifier
        IPocketUpgrade upgrade = pocket.getUpgrade( stack );
        name.append( '|' );
        if( upgrade != null ) name.append( upgrade.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes disks by colour
     */
    private static final ISubtypeInterpreter diskSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemDiskLegacy) ) return "";

        ItemDiskLegacy disk = (ItemDiskLegacy) item;

        int colour = disk.getColour( stack );
        return colour == -1 ? "" : String.format( "%06x", colour );
    };
}
