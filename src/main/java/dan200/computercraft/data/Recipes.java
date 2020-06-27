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
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.block.Blocks;
import net.minecraft.data.*;
import net.minecraft.item.*;
import net.minecraft.tags.Tag;
import net.minecraft.util.IItemProvider;
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
    private void diskColours( @Nonnull Consumer<IFinishedRecipe> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeBuilder
                .shapelessRecipe( Registry.ModItems.DISK.get() )
                .addIngredient( Tags.Items.DUSTS_REDSTONE )
                .addIngredient( Items.PAPER )
                .addIngredient( DyeItem.getItem( ofColour( colour ) ) )
                .setGroup( "computercraft:disk" )
                .addCriterion( "has_drive", inventoryChange( Registry.ModBlocks.DISK_DRIVE.get() ) )
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
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
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
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .build(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    private void basicRecipes( @Nonnull Consumer<IFinishedRecipe> add )
    {
        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModItems.CABLE.get(), 6 )
            .patternLine( " # " )
            .patternLine( "#R#" )
            .patternLine( " # " )
            .key( '#', Tags.Items.STONE )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .addCriterion( "has_modem", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.COMPUTER_NORMAL.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "#G#" )
            .key( '#', Tags.Items.STONE )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_redstone", inventoryChange( Tags.Items.DUSTS_REDSTONE ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.COMPUTER_ADVANCED.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "#G#" )
            .key( '#', Tags.Items.INGOTS_GOLD )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_components", inventoryChange( Items.REDSTONE, Items.GOLD_INGOT ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.COMPUTER_COMMAND.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "#G#" )
            .key( '#', Tags.Items.INGOTS_GOLD )
            .key( 'R', Blocks.COMMAND_BLOCK )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_components", inventoryChange( Blocks.COMMAND_BLOCK ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.DISK_DRIVE.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "#R#" )
            .key( '#', Tags.Items.STONE )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.MONITOR_NORMAL.get() )
            .patternLine( "###" )
            .patternLine( "#G#" )
            .patternLine( "###" )
            .key( '#', Tags.Items.STONE )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.MONITOR_ADVANCED.get(), 4 )
            .patternLine( "###" )
            .patternLine( "#G#" )
            .patternLine( "###" )
            .key( '#', Tags.Items.INGOTS_GOLD )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModItems.POCKET_COMPUTER_NORMAL.get() )
            .patternLine( "###" )
            .patternLine( "#A#" )
            .patternLine( "#G#" )
            .key( '#', Tags.Items.STONE )
            .key( 'A', Items.GOLDEN_APPLE )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .addCriterion( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() )
            .patternLine( "###" )
            .patternLine( "#A#" )
            .patternLine( "#G#" )
            .key( '#', Tags.Items.INGOTS_GOLD )
            .key( 'A', Items.GOLDEN_APPLE )
            .key( 'G', Tags.Items.GLASS_PANES )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .addCriterion( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.PRINTER.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "#D#" )
            .key( '#', Tags.Items.STONE )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .key( 'D', Tags.Items.DYES )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.SPEAKER.get() )
            .patternLine( "###" )
            .patternLine( "#N#" )
            .patternLine( "#R#" )
            .key( '#', Tags.Items.STONE )
            .key( 'N', Blocks.NOTE_BLOCK )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModItems.WIRED_MODEM.get() )
            .patternLine( "###" )
            .patternLine( "#R#" )
            .patternLine( "###" )
            .key( '#', Tags.Items.STONE )
            .key( 'R', Tags.Items.DUSTS_REDSTONE )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .addCriterion( "has_cable", inventoryChange( Registry.ModItems.CABLE.get() ) )
            .build( add );

        ShapelessRecipeBuilder
            .shapelessRecipe( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .addIngredient( Registry.ModItems.WIRED_MODEM.get() )
            .addCriterion( "has_modem", inventoryChange( CCTags.WIRED_MODEM ) )
            .build( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_from" ) );
        ShapelessRecipeBuilder
            .shapelessRecipe( Registry.ModItems.WIRED_MODEM.get() )
            .addIngredient( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .addCriterion( "has_modem", inventoryChange( CCTags.WIRED_MODEM ) )
            .build( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_to" ) );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() )
            .patternLine( "###" )
            .patternLine( "#E#" )
            .patternLine( "###" )
            .key( '#', Tags.Items.STONE )
            .key( 'E', Tags.Items.ENDER_PEARLS )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .build( add );

        ShapedRecipeBuilder
            .shapedRecipe( Registry.ModBlocks.WIRELESS_MODEM_ADVANCED.get() )
            .patternLine( "###" )
            .patternLine( "#E#" )
            .patternLine( "###" )
            .key( '#', Tags.Items.INGOTS_GOLD )
            .key( 'E', Items.ENDER_EYE )
            .addCriterion( "has_computer", inventoryChange( CCTags.COMPUTER ) )
            .addCriterion( "has_wireless", inventoryChange( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() ) )
            .build( add );
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }

    private static InventoryChangeTrigger.Instance inventoryChange( Tag<Item> stack )
    {
        return InventoryChangeTrigger.Instance.forItems( ItemPredicate.Builder.create().tag( stack ).build() );
    }

    private static InventoryChangeTrigger.Instance inventoryChange( IItemProvider... stack )
    {
        return InventoryChangeTrigger.Instance.forItems( stack );
    }
}
