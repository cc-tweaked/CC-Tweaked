/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.*;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

public class Recipes extends RecipeProvider
{
    public Recipes( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void registerRecipes( @Nonnull Consumer<IFinishedRecipe> add )
    {
        diskColours( add );
        pocketUpgrades( add );
        turtleUpgrades( add );
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param add The callback to add recipes.
     */
    private void diskColours( @Nonnull Consumer<IFinishedRecipe> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeBuilder
                .shapelessRecipe( ComputerCraft.Items.disk )
                .addIngredient( Tags.Items.DUSTS_REDSTONE )
                .addIngredient( Items.PAPER )
                .addIngredient( DyeItem.getItem( ofColour( colour ) ) )
                .setGroup( "computercraft:disk" )
                .addCriterion( "has_drive", InventoryChangeTrigger.Instance.forItems( ComputerCraft.Blocks.diskDrive ) )
                .build( RecipeWrapper.wrap(
                    ImpostorShapelessRecipe.SERIALIZER, add,
                    x -> x.putInt( "color", colour.getHex() )
                ), new ResourceLocation( ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1) ) );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades( @Nonnull Consumer<IFinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, -1, null );
                ShapedRecipeBuilder
                    .shapedRecipe( result.getItem() )
                    .setGroup( String.format( "%s:turtle_%s", ComputerCraft.MOD_ID, nameId ) )
                    .patternLine( "#T" )
                    .key( '#', base.getItem() )
                    .key( 'T', upgrade.getCraftingItem().getItem() )
                    .addCriterion( "has_items",
                        InventoryChangeTrigger.Instance.forItems( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .build(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "turtle_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void pocketUpgrades( @Nonnull Consumer<IFinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = PocketComputerItemFactory.create( -1, null, -1, family, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = PocketComputerItemFactory.create( -1, null, -1, family, null );
                ShapedRecipeBuilder
                    .shapedRecipe( result.getItem() )
                    .setGroup( String.format( "%s:pocket_%s", ComputerCraft.MOD_ID, nameId ) )
                    .patternLine( "#" )
                    .patternLine( "P" )
                    .key( '#', base.getItem() )
                    .key( 'P', upgrade.getCraftingItem().getItem() )
                    .addCriterion( "has_items",
                        InventoryChangeTrigger.Instance.forItems( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .build(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }
}
