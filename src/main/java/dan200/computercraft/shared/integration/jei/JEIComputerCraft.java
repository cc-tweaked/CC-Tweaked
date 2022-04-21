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
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

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
            for( ITurtleUpgrade upgrade : TurtleUpgrades.instance().getUpgrades() )
            {
                upgradeItems.add( TurtleItemFactory.create( -1, null, -1, family, null, upgrade, 0, null ) );
            }

            for( IPocketUpgrade upgrade : PocketUpgrades.instance().getUpgrades() )
            {
                upgradeItems.add( PocketComputerItemFactory.create( -1, null, -1, family, upgrade ) );
            }
        }

        if( !upgradeItems.isEmpty() )
        {
            runtime.getIngredientManager().addIngredientsAtRuntime( VanillaTypes.ITEM, upgradeItems );
        }

        // Hide all upgrade recipes
        IRecipeCategory<?> category = registry.getRecipeCategory( VanillaRecipeCategoryUid.CRAFTING, false );
        if( category != null )
        {
            for( Object wrapper : registry.getRecipes( category, List.of(), false ) )
            {
                if( !(wrapper instanceof Recipe) ) continue;
                ResourceLocation id = ((Recipe<?>) wrapper).getId();
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
