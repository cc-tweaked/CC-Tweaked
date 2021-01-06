/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.jei;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.integration.jei.RecipeResolver.MAIN_FAMILIES;

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
        subtypeRegistry.registerSubtypeInterpreter( Registry.ModItems.TURTLE_NORMAL.get(), turtleSubtype );
        subtypeRegistry.registerSubtypeInterpreter( Registry.ModItems.TURTLE_ADVANCED.get(), turtleSubtype );

        subtypeRegistry.registerSubtypeInterpreter( Registry.ModItems.POCKET_COMPUTER_NORMAL.get(), pocketSubtype );
        subtypeRegistry.registerSubtypeInterpreter( Registry.ModItems.POCKET_COMPUTER_ADVANCED.get(), pocketSubtype );

        subtypeRegistry.registerSubtypeInterpreter( Registry.ModItems.DISK.get(), diskSubtype );
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
        List<ItemStack> upgradeItems = new ArrayList<>();
        for( ComputerFamily family : MAIN_FAMILIES )
        {
            TurtleUpgrades.getUpgrades()
                .filter( x -> TurtleUpgrades.suitableForFamily( family, x ) )
                .map( x -> TurtleItemFactory.create( -1, null, -1, family, null, x, 0, null ) )
                .forEach( upgradeItems::add );

            for( IPocketUpgrade upgrade : PocketUpgrades.getUpgrades() )
            {
                upgradeItems.add( PocketComputerItemFactory.create( -1, null, -1, family, upgrade ) );
            }
        }

        runtime.getIngredientManager().addIngredientsAtRuntime( VanillaTypes.ITEM, upgradeItems );

        // Hide all upgrade recipes
        IRecipeCategory<?> category = (IRecipeCategory<?>) registry.getRecipeCategory( VanillaRecipeCategoryUid.CRAFTING );
        if( category != null )
        {
            for( Object wrapper : registry.getRecipes( category ) )
            {
                if( !(wrapper instanceof IRecipe) ) continue;
                ResourceLocation id = ((IRecipe) wrapper).getId();
                if( !id.getNamespace().equals( ComputerCraft.MOD_ID ) ) continue;

                String path = id.getPath();
                if( path.startsWith( "turtle_normal/" ) || path.startsWith( "turtle_advanced/" )
                    || path.startsWith( "pocket_normal/" ) || path.startsWith( "pocket_advanced/" ) )
                {
                    registry.hideRecipe( wrapper, VanillaRecipeCategoryUid.CRAFTING );
                }
            }
        }
    }

    /**
     * Distinguishes turtles by upgrades and family.
     */
    private static final ISubtypeInterpreter turtleSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ITurtleItem) ) return "";

        ITurtleItem turtle = (ITurtleItem) item;
        StringBuilder name = new StringBuilder();

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
    private static final ISubtypeInterpreter pocketSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemPocketComputer) ) return "";

        StringBuilder name = new StringBuilder();

        // Add the upgrade to the identifier
        IPocketUpgrade upgrade = ItemPocketComputer.getUpgrade( stack );
        if( upgrade != null ) name.append( upgrade.getUpgradeID() );

        return name.toString();
    };

    /**
     * Distinguishes disks by colour.
     */
    private static final ISubtypeInterpreter diskSubtype = stack -> {
        Item item = stack.getItem();
        if( !(item instanceof ItemDisk) ) return "";

        ItemDisk disk = (ItemDisk) item;

        int colour = disk.getColour( stack );
        return colour == -1 ? "" : String.format( "%06x", colour );
    };
}
