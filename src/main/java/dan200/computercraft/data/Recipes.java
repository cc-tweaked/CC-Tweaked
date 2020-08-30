/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.data.Tags.CCTags;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.data.*;
import net.minecraft.data.server.RecipesProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonFactory;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonFactory;
import net.minecraft.item.*;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.tag.Tag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

public class Recipes extends RecipesProvider
{
    public Recipes( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void generate( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        basicRecipes( add );
        diskColours( add );
        pocketUpgrades( add );
        turtleUpgrades( add );
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param add The callback to add recipes.
     */
    private void diskColours( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeJsonFactory
                .create( Registry.ModItems.DISK.get() )
                .input( Tags.Items.DUSTS_REDSTONE )
                .input( Items.PAPER )
                .input( DyeItem.byColor( ofColour( colour ) ) )
                .group( "computercraft:disk" )
                .criterion( "has_drive", inventoryChange( Registry.ModBlocks.DISK_DRIVE.get() ) )
                .offerTo( RecipeWrapper.wrap(
                    ImpostorShapelessRecipe.SERIALIZER, add,
                    x -> x.putInt( "color", colour.getHex() )
                ), new Identifier( ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1) ) );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, -1, null );
                ShapedRecipeJsonFactory
                    .create( result.getItem() )
                    .group( String.format( "%s:turtle_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#T" )
                    .input( '#', base.getItem() )
                    .input( 'T', upgrade.getCraftingItem().getItem() )
                    .criterion( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .offerTo(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new Identifier( ComputerCraft.MOD_ID, String.format( "turtle_%s/%s/%s",
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
    private void pocketUpgrades( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = PocketComputerItemFactory.create( -1, null, -1, family, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = PocketComputerItemFactory.create( -1, null, -1, family, null );
                ShapedRecipeJsonFactory
                    .create( result.getItem() )
                    .group( String.format( "%s:pocket_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#" )
                    .pattern( "P" )
                    .input( '#', base.getItem() )
                    .input( 'P', upgrade.getCraftingItem().getItem() )
                    .criterion( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .offerTo(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new Identifier( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    private void basicRecipes( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        ShapedRecipeJsonFactory
            .create( Registry.ModItems.CABLE.get(), 6 )
            .pattern( " # " )
            .pattern( "#R#" )
            .pattern( " # " )
            .input( '#', Tags.Items.STONE )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .criterion( "has_modem", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', Tags.Items.STONE )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_redstone", inventoryChange( Tags.Items.DUSTS_REDSTONE ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', Tags.Items.INGOTS_GOLD )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_components", inventoryChange( Items.REDSTONE, Items.GOLD_INGOT ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_COMMAND.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', Tags.Items.INGOTS_GOLD )
            .input( 'R', Blocks.COMMAND_BLOCK )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_components", inventoryChange( Blocks.COMMAND_BLOCK ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.DISK_DRIVE.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#R#" )
            .input( '#', Tags.Items.STONE )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.MONITOR_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .input( '#', Tags.Items.STONE )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.MONITOR_ADVANCED.get(), 4 )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .input( '#', Tags.Items.INGOTS_GOLD )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.POCKET_COMPUTER_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .input( '#', Tags.Items.STONE )
            .input( 'A', Items.GOLDEN_APPLE )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .criterion( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .input( '#', Tags.Items.INGOTS_GOLD )
            .input( 'A', Items.GOLDEN_APPLE )
            .input( 'G', Tags.Items.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .criterion( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.PRINTER.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#D#" )
            .input( '#', Tags.Items.STONE )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .input( 'D', Tags.Items.DYES )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.SPEAKER.get() )
            .pattern( "###" )
            .pattern( "#N#" )
            .pattern( "#R#" )
            .input( '#', Tags.Items.STONE )
            .input( 'N', Blocks.NOTE_BLOCK )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.WIRED_MODEM.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "###" )
            .input( '#', Tags.Items.STONE )
            .input( 'R', Tags.Items.DUSTS_REDSTONE )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .criterion( "has_cable", inventoryChange( Registry.ModItems.CABLE.get() ) )
            .offerTo( add );

        ShapelessRecipeJsonFactory
            .create( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .input( Registry.ModItems.WIRED_MODEM.get() )
            .criterion( "has_modem", inventoryChange( CCTags.WIRED_MODEM ) )
            .offerTo( add, new Identifier( ComputerCraft.MOD_ID, "wired_modem_full_from" ) );
        ShapelessRecipeJsonFactory
            .create( Registry.ModItems.WIRED_MODEM.get() )
            .input( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .criterion( "has_modem", inventoryChange( CCTags.WIRED_MODEM ) )
            .offerTo( add, new Identifier( ComputerCraft.MOD_ID, "wired_modem_full_to" ) );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .input( '#', Tags.Items.STONE )
            .input( 'E', Tags.Items.ENDER_PEARLS )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.WIRELESS_MODEM_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .input( '#', Tags.Items.INGOTS_GOLD )
            .input( 'E', Items.ENDER_EYE )
            .criterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .criterion( "has_wireless", inventoryChange( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() ) )
            .offerTo( add );
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }

    private static InventoryChangedCriterion.Conditions inventoryChange( Tag<Item> stack )
    {
        return InventoryChangedCriterion.Conditions.items( ItemPredicate.Builder.create().tag( stack ).build() );
    }

    private static InventoryChangedCriterion.Conditions inventoryChange( ItemConvertible... stack )
    {
        return InventoryChangedCriterion.Conditions.items( stack );
    }
}
