/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RecipeIngredients;
import dan200.computercraft.shared.platform.Registries;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

class RecipeGenerator extends RecipeProvider {
    private final RecipeIngredients ingredients = PlatformHelper.get().getRecipeIngredients();
    private final TurtleUpgradeDataProvider turtleUpgrades;
    private final PocketUpgradeDataProvider pocketUpgrades;

    RecipeGenerator(DataGenerator generator, TurtleUpgradeDataProvider turtleUpgrades, PocketUpgradeDataProvider pocketUpgrades) {
        super(generator);

        this.turtleUpgrades = turtleUpgrades;
        this.pocketUpgrades = pocketUpgrades;
    }

    @Override
    protected void buildCraftingRecipes(@Nonnull Consumer<FinishedRecipe> add) {
        basicRecipes(add);
        diskColours(add);
        pocketUpgrades(add);
        turtleUpgrades(add);

        addSpecial(add, ModRegistry.RecipeSerializers.PRINTOUT.get());
        addSpecial(add, ModRegistry.RecipeSerializers.DISK.get());
        addSpecial(add, ModRegistry.RecipeSerializers.DYEABLE_ITEM.get());
        addSpecial(add, ModRegistry.RecipeSerializers.TURTLE_UPGRADE.get());
        addSpecial(add, ModRegistry.RecipeSerializers.POCKET_COMPUTER_UPGRADE.get());
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param add The callback to add recipes.
     */
    private void diskColours(@Nonnull Consumer<FinishedRecipe> add) {
        for (var colour : Colour.VALUES) {
            ShapelessRecipeBuilder
                .shapeless(ModRegistry.Items.DISK.get())
                .requires(ingredients.redstone())
                .requires(Items.PAPER)
                .requires(DyeItem.byColor(ofColour(colour)))
                .group("computercraft:disk")
                .unlockedBy("has_drive", inventoryChange(ModRegistry.Blocks.DISK_DRIVE.get()))
                .save(
                    RecipeWrapper.wrap(ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get(), add)
                        .withResultTag(x -> x.putInt(IColouredItem.NBT_COLOUR, colour.getHex())),
                    new ResourceLocation(ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1))
                );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades(@Nonnull Consumer<FinishedRecipe> add) {
        for (var family : ComputerFamily.values()) {
            var base = TurtleItemFactory.create(-1, null, -1, family, null, null, 0, null);
            if (base.isEmpty()) continue;

            var nameId = family.name().toLowerCase(Locale.ROOT);

            for (var upgrade : turtleUpgrades.getGeneratedUpgrades()) {
                var result = TurtleItemFactory.create(-1, null, -1, family, null, upgrade, -1, null);
                ShapedRecipeBuilder
                    .shaped(result.getItem())
                    .group(String.format("%s:turtle_%s", ComputerCraft.MOD_ID, nameId))
                    .pattern("#T")
                    .define('T', base.getItem())
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items",
                        inventoryChange(base.getItem(), upgrade.getCraftingItem().getItem()))
                    .save(
                        RecipeWrapper.wrap(ModRegistry.RecipeSerializers.IMPOSTOR_SHAPED.get(), add).withResultTag(result.getTag()),
                        new ResourceLocation(ComputerCraft.MOD_ID, String.format("turtle_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ))
                    );
            }
        }
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void pocketUpgrades(@Nonnull Consumer<FinishedRecipe> add) {
        for (var family : ComputerFamily.values()) {
            var base = PocketComputerItemFactory.create(-1, null, -1, family, null);
            if (base.isEmpty()) continue;

            var nameId = family.name().toLowerCase(Locale.ROOT);

            for (var upgrade : pocketUpgrades.getGeneratedUpgrades()) {
                var result = PocketComputerItemFactory.create(-1, null, -1, family, upgrade);
                ShapedRecipeBuilder
                    .shaped(result.getItem())
                    .group(String.format("%s:pocket_%s", ComputerCraft.MOD_ID, nameId))
                    .pattern("#")
                    .pattern("P")
                    .define('P', base.getItem())
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items",
                        inventoryChange(base.getItem(), upgrade.getCraftingItem().getItem()))
                    .save(
                        RecipeWrapper.wrap(ModRegistry.RecipeSerializers.IMPOSTOR_SHAPED.get(), add).withResultTag(result.getTag()),
                        new ResourceLocation(ComputerCraft.MOD_ID, String.format("pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ))
                    );
            }
        }
    }

    private void basicRecipes(@Nonnull Consumer<FinishedRecipe> add) {
        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.CABLE.get(), 6)
            .pattern(" # ")
            .pattern("#R#")
            .pattern(" # ")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_redstone", inventoryChange(itemPredicate(ingredients.redstone())))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ingredients.redstone()), itemPredicate(ingredients.goldIngot())))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .save(
                RecipeWrapper.wrap(ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get(), add).withExtraData(family(ComputerFamily.ADVANCED)),
                new ResourceLocation(ComputerCraft.MOD_ID, "computer_advanced_upgrade")
            );

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.COMPUTER_COMMAND.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', Blocks.COMMAND_BLOCK)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(Blocks.COMMAND_BLOCK))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.TURTLE_NORMAL.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.ironIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .save(RecipeWrapper.wrap(ModRegistry.RecipeSerializers.TURTLE.get(), add).withExtraData(family(ComputerFamily.NORMAL)));

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .save(RecipeWrapper.wrap(ModRegistry.RecipeSerializers.TURTLE.get(), add).withExtraData(family(ComputerFamily.ADVANCED)));

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern(" B ")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .define('B', ingredients.goldBlock())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.TURTLE_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .save(
                RecipeWrapper.wrap(ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get(), add).withExtraData(family(ComputerFamily.ADVANCED)),
                new ResourceLocation(ComputerCraft.MOD_ID, "turtle_advanced_upgrade")
            );

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.DISK_DRIVE.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.MONITOR_NORMAL.get())
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.MONITOR_ADVANCED.get(), 4)
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.stone())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_apple", inventoryChange(Items.GOLDEN_APPLE))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_apple", inventoryChange(Items.GOLDEN_APPLE))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .save(
                RecipeWrapper.wrap(ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get(), add).withExtraData(family(ComputerFamily.ADVANCED)),
                new ResourceLocation(ComputerCraft.MOD_ID, "pocket_computer_advanced_upgrade")
            );

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.PRINTER.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#D#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('D', ingredients.dye())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.SPEAKER.get())
            .pattern("###")
            .pattern("#N#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('N', Blocks.NOTE_BLOCK)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Items.WIRED_MODEM.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_cable", inventoryChange(ModRegistry.Items.CABLE.get()))
            .save(add);

        ShapelessRecipeBuilder
            .shapeless(ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .requires(ModRegistry.Items.WIRED_MODEM.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraft.MOD_ID, "wired_modem_full_from"));
        ShapelessRecipeBuilder
            .shapeless(ModRegistry.Items.WIRED_MODEM.get())
            .requires(ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraft.MOD_ID, "wired_modem_full_to"));

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('E', ingredients.enderPearl())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('E', Items.ENDER_EYE)
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_wireless", inventoryChange(ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get()))
            .save(add);

        ShapelessRecipeBuilder
            .shapeless(Items.PLAYER_HEAD)
            .requires(ingredients.head())
            .requires(ModRegistry.Items.MONITOR_NORMAL.get())
            .unlockedBy("has_monitor", inventoryChange(ModRegistry.Items.MONITOR_NORMAL.get()))
            .save(
                RecipeWrapper.wrap(RecipeSerializer.SHAPELESS_RECIPE, add)
                    .withResultTag(playerHead("Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c")),
                new ResourceLocation(ComputerCraft.MOD_ID, "skull_cloudy")
            );

        ShapelessRecipeBuilder
            .shapeless(Items.PLAYER_HEAD)
            .requires(ingredients.head())
            .requires(ModRegistry.Items.COMPUTER_ADVANCED.get())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_ADVANCED.get()))
            .save(
                RecipeWrapper.wrap(RecipeSerializer.SHAPELESS_RECIPE, add)
                    .withResultTag(playerHead("dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb")),
                new ResourceLocation(ComputerCraft.MOD_ID, "skull_dan200")
            );

        ShapelessRecipeBuilder
            .shapeless(ModRegistry.Items.PRINTED_PAGES.get())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 2)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .save(RecipeWrapper.wrap(ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get(), add));

        ShapelessRecipeBuilder
            .shapeless(ModRegistry.Items.PRINTED_BOOK.get())
            .requires(ingredients.leather())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 1)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .save(RecipeWrapper.wrap(ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get(), add));
    }

    private static DyeColor ofColour(Colour colour) {
        return DyeColor.byId(15 - colour.ordinal());
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange(TagKey<Item> stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(itemPredicate(stack));
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange(ItemLike... stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(stack);
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange(ItemPredicate... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(items);
    }

    private static ItemPredicate itemPredicate(ItemLike item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(TagKey<Item> item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(Ingredient ingredient) {
        var json = ingredient.toJson();
        if (!(json instanceof JsonObject object)) throw new IllegalStateException("Unknown ingredient " + json);

        if (object.has("item")) {
            return itemPredicate(ShapedRecipe.itemFromJson(object));
        } else if (object.has("tag")) {
            return itemPredicate(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(GsonHelper.getAsString(object, "tag"))));
        } else {
            throw new IllegalArgumentException("Unknown ingredient " + json);
        }
    }

    private static CompoundTag playerHead(String name, String uuid) {
        var owner = new CompoundTag();
        owner.putString("Name", name);
        owner.putString("Id", uuid);

        var tag = new CompoundTag();
        tag.put("SkullOwner", owner);
        return tag;
    }

    private static Consumer<JsonObject> family(ComputerFamily family) {
        return json -> json.addProperty("family", family.toString());
    }

    private static void addSpecial(Consumer<FinishedRecipe> add, SimpleRecipeSerializer<?> special) {
        SpecialRecipeBuilder.special(special).save(add, Registries.RECIPE_SERIALIZERS.getKey(special).toString());
    }
}
