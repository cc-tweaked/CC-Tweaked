/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

class RecipeGenerator extends RecipeProvider
{
    private final TurtleUpgradeDataProvider turtleUpgrades;
    private final PocketUpgradeDataProvider pocketUpgrades;

    RecipeGenerator( DataGenerator generator, TurtleUpgradeDataProvider turtleUpgrades, PocketUpgradeDataProvider pocketUpgrades )
    {
        super( generator );

        this.turtleUpgrades = turtleUpgrades;
        this.pocketUpgrades = pocketUpgrades;
    }

    @Override
    protected void buildCraftingRecipes( @Nonnull Consumer<FinishedRecipe> add )
    {
        basicRecipes( add );
        diskColours( add );
        pocketUpgrades( add );
        turtleUpgrades( add );

        addSpecial( add, PrintoutRecipe.SERIALIZER );
        addSpecial( add, DiskRecipe.SERIALIZER );
        addSpecial( add, ColourableRecipe.SERIALIZER );
        addSpecial( add, TurtleUpgradeRecipe.SERIALIZER );
        addSpecial( add, PocketComputerUpgradeRecipe.SERIALIZER );
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param add The callback to add recipes.
     */
    private void diskColours( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeBuilder
                .shapeless( Registry.ModItems.DISK.get() )
                .requires( Tags.Items.DUSTS_REDSTONE )
                .requires( Items.PAPER )
                .requires( DyeItem.byColor( ofColour( colour ) ) )
                .group( "computercraft:disk" )
                .unlockedBy( "has_drive", inventoryChange( Registry.ModBlocks.DISK_DRIVE.get() ) )
                .save( RecipeWrapper.wrap(
                    ImpostorShapelessRecipe.SERIALIZER, add,
                    x -> x.putInt( IColouredItem.NBT_COLOUR, colour.getHex() )
                ), new ResourceLocation( ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1) ) );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            for( var upgrade : turtleUpgrades.getGeneratedUpgrades() )
            {
                ItemStack result = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, -1, null );
                ShapedRecipeBuilder
                    .shaped( result.getItem() )
                    .group( String.format( "%s:turtle_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#T" )
                    .define( 'T', base.getItem() )
                    .define( '#', upgrade.getCraftingItem().getItem() )
                    .unlockedBy( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .save(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "turtle_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            }
        }
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void pocketUpgrades( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = PocketComputerItemFactory.create( -1, null, -1, family, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            for( var upgrade : pocketUpgrades.getGeneratedUpgrades() )
            {
                ItemStack result = PocketComputerItemFactory.create( -1, null, -1, family, upgrade );
                ShapedRecipeBuilder
                    .shaped( result.getItem() )
                    .group( String.format( "%s:pocket_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#" )
                    .pattern( "P" )
                    .define( 'P', base.getItem() )
                    .define( '#', upgrade.getCraftingItem().getItem() )
                    .unlockedBy( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .save(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            }
        }
    }

    private void basicRecipes( @Nonnull Consumer<FinishedRecipe> add )
    {
        ShapedRecipeBuilder
            .shaped( Registry.ModItems.CABLE.get(), 6 )
            .pattern( " # " )
            .pattern( "#R#" )
            .pattern( " # " )
            .define( '#', Tags.Items.STONE )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.COMPUTER_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', Tags.Items.STONE )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_redstone", inventoryChange( Tags.Items.DUSTS_REDSTONE ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.COMPUTER_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', Tags.Items.INGOTS_GOLD )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_components", inventoryChange( Items.REDSTONE, Items.GOLD_INGOT ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.COMPUTER_COMMAND.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', Tags.Items.INGOTS_GOLD )
            .define( 'R', Blocks.COMMAND_BLOCK )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_components", inventoryChange( Blocks.COMMAND_BLOCK ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.DISK_DRIVE.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#R#" )
            .define( '#', Tags.Items.STONE )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.MONITOR_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .define( '#', Tags.Items.STONE )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.MONITOR_ADVANCED.get(), 4 )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .define( '#', Tags.Items.INGOTS_GOLD )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModItems.POCKET_COMPUTER_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .define( '#', Tags.Items.STONE )
            .define( 'A', Items.GOLDEN_APPLE )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModItems.POCKET_COMPUTER_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .define( '#', Tags.Items.INGOTS_GOLD )
            .define( 'A', Items.GOLDEN_APPLE )
            .define( 'G', Tags.Items.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.PRINTER.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#D#" )
            .define( '#', Tags.Items.STONE )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .define( 'D', Tags.Items.DYES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.SPEAKER.get() )
            .pattern( "###" )
            .pattern( "#N#" )
            .pattern( "#R#" )
            .define( '#', Tags.Items.STONE )
            .define( 'N', Blocks.NOTE_BLOCK )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModItems.WIRED_MODEM.get() )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "###" )
            .define( '#', Tags.Items.STONE )
            .define( 'R', Tags.Items.DUSTS_REDSTONE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_cable", inventoryChange( Registry.ModItems.CABLE.get() ) )
            .save( add );

        ShapelessRecipeBuilder
            .shapeless( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .requires( Registry.ModItems.WIRED_MODEM.get() )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_from" ) );
        ShapelessRecipeBuilder
            .shapeless( Registry.ModItems.WIRED_MODEM.get() )
            .requires( Registry.ModBlocks.WIRED_MODEM_FULL.get() )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_to" ) );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .define( '#', Tags.Items.STONE )
            .define( 'E', Tags.Items.ENDER_PEARLS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( Registry.ModBlocks.WIRELESS_MODEM_ADVANCED.get() )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .define( '#', Tags.Items.INGOTS_GOLD )
            .define( 'E', Items.ENDER_EYE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_wireless", inventoryChange( Registry.ModBlocks.WIRELESS_MODEM_NORMAL.get() ) )
            .save( add );

        ShapelessRecipeBuilder
            .shapeless( Items.PLAYER_HEAD )
            .requires( Tags.Items.HEADS )
            .requires( Registry.ModItems.MONITOR_NORMAL.get() )
            .unlockedBy( "has_monitor", inventoryChange( Registry.ModItems.MONITOR_NORMAL.get() ) )
            .save(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS_RECIPE, add, playerHead( "Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c" ) ),
                new ResourceLocation( ComputerCraft.MOD_ID, "skull_cloudy" )
            );

        ShapelessRecipeBuilder
            .shapeless( Items.PLAYER_HEAD )
            .requires( Tags.Items.HEADS )
            .requires( Registry.ModItems.COMPUTER_ADVANCED.get() )
            .unlockedBy( "has_computer", inventoryChange( Registry.ModItems.COMPUTER_ADVANCED.get() ) )
            .save(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS_RECIPE, add, playerHead( "dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb" ) ),
                new ResourceLocation( ComputerCraft.MOD_ID, "skull_dan200" )
            );

        ShapelessRecipeBuilder
            .shapeless( Registry.ModItems.PRINTED_PAGES.get() )
            .requires( Registry.ModItems.PRINTED_PAGE.get(), 2 )
            .requires( Tags.Items.STRING )
            .unlockedBy( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER.get() ) )
            .save( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );

        ShapelessRecipeBuilder
            .shapeless( Registry.ModItems.PRINTED_BOOK.get() )
            .requires( Tags.Items.LEATHER )
            .requires( Registry.ModItems.PRINTED_PAGE.get(), 1 )
            .requires( Tags.Items.STRING )
            .unlockedBy( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER.get() ) )
            .save( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange( TagKey<Item> stack )
    {
        return InventoryChangeTrigger.TriggerInstance.hasItems( ItemPredicate.Builder.item().of( stack ).build() );
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange( ItemLike... stack )
    {
        return InventoryChangeTrigger.TriggerInstance.hasItems( stack );
    }

    private static CompoundTag playerHead( String name, String uuid )
    {
        CompoundTag owner = new CompoundTag();
        owner.putString( "Name", name );
        owner.putString( "Id", uuid );

        CompoundTag tag = new CompoundTag();
        tag.put( "SkullOwner", owner );
        return tag;
    }

    private static void addSpecial( Consumer<FinishedRecipe> add, SimpleRecipeSerializer<?> special )
    {
        SpecialRecipeBuilder.special( special ).save( add, special.getRegistryName().toString() );
    }
}
