// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.data.recipe.ShapedSpecBuilder;
import dan200.computercraft.data.recipe.ShapelessSpecBuilder;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.ClearColourRecipe;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RecipeIngredients;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapedRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapelessRecipe;
import dan200.computercraft.shared.recipe.TransformShapedRecipe;
import dan200.computercraft.shared.recipe.TransformShapelessRecipe;
import dan200.computercraft.shared.recipe.function.CopyComponents;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

final class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    private final RecipeIngredients ingredients = PlatformHelper.get().getRecipeIngredients();

    private final CompletableFuture<HolderLookup.Provider> registries;

    RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.registries = registries;
    }

    private HolderLookup.Provider registries() {
        try {
            return registries.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted");
        } catch (ExecutionException e) {
            var cause = e.getCause();
            throw cause instanceof RuntimeException rt ? rt : new RuntimeException("Unexpected error", cause);
        }
    }

    @Override
    public void buildRecipes(RecipeOutput add) {
        var registries = registries();

        basicRecipes(add);
        diskColours(add);
        pocketUpgrades(add, registries);
        turtleUpgrades(add, registries);
        turtleOverlays(add);

        addSpecial(add, new PrintoutRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new DiskRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new ColourableRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new ClearColourRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new TurtleUpgradeRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new PocketComputerUpgradeRecipe(CraftingBookCategory.MISC));
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param output The callback to add recipes.
     */
    private void diskColours(RecipeOutput output) {
        for (var colour : Colour.VALUES) {
            ShapelessSpecBuilder
                .shapeless(RecipeCategory.REDSTONE, DataComponentUtil.createStack(ModRegistry.Items.DISK.get(), DataComponents.DYED_COLOR, new DyedItemColor(colour.getHex(), false)))
                .requires(ingredients.redstone())
                .requires(Items.PAPER)
                .requires(DyeItem.byColor(ofColour(colour)))
                .group("computercraft:disk")
                .unlockedBy("has_drive", inventoryChange(ModRegistry.Blocks.DISK_DRIVE.get()))
                .build(ImpostorShapelessRecipe::new)
                .save(output, new ResourceLocation(ComputerCraftAPI.MOD_ID, "disk_" + (colour.ordinal() + 1)));
        }
    }

    private static List<TurtleItem> turtleItems() {
        return List.of(ModRegistry.Items.TURTLE_NORMAL.get(), ModRegistry.Items.TURTLE_ADVANCED.get());
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add        The callback to add recipes.
     * @param registries The currently available registries.
     */
    private void turtleUpgrades(RecipeOutput add, HolderLookup.Provider registries) {
        for (var turtleItem : turtleItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, turtleItem);

            registries.lookup(ModRegistry.TURTLE_UPGRADE).map(HolderLookup::listElements).orElse(Stream.empty()).forEach(upgradeHolder -> {
                var upgrade = upgradeHolder.value();
                ShapedSpecBuilder
                    .shaped(RecipeCategory.REDSTONE, DataComponentUtil.createStack(turtleItem, ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), UpgradeData.ofDefault(upgradeHolder)))
                    .group(name.toString())
                    .pattern("#T")
                    .define('T', turtleItem)
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", inventoryChange(turtleItem, upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        add,
                        name.withSuffix(String.format("/%s/%s", upgradeHolder.key().location().getNamespace(), upgradeHolder.key().location().getPath()))
                    );
            });
        }
    }

    private static List<PocketComputerItem> pocketComputerItems() {
        return List.of(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add        The callback to add recipes.
     * @param registries The currently available registries.
     */
    private void pocketUpgrades(RecipeOutput add, HolderLookup.Provider registries) {
        for (var pocket : pocketComputerItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, pocket).withPath(x -> x.replace("pocket_computer_", "pocket_"));

            registries.lookup(ModRegistry.POCKET_UPGRADE).map(HolderLookup::listElements).orElse(Stream.empty()).forEach(upgradeHolder -> {
                var upgrade = upgradeHolder.value();
                ShapedSpecBuilder
                    .shaped(RecipeCategory.REDSTONE, DataComponentUtil.createStack(pocket, ModRegistry.DataComponents.POCKET_UPGRADE.get(), UpgradeData.ofDefault(upgradeHolder)))
                    .group(name.toString())
                    .pattern("#")
                    .pattern("P")
                    .define('P', pocket)
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", inventoryChange(pocket, upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        add,
                        name.withSuffix(String.format("/%s/%s", upgradeHolder.key().location().getNamespace(), upgradeHolder.key().location().getPath()))
                    );
            });
        }
    }

    private void turtleOverlays(RecipeOutput add) {
        turtleOverlay(add, "turtle_trans_overlay", x -> x
            .unlockedBy("has_dye", inventoryChange(itemPredicate(ingredients.dye())))
            .requires(ColourUtils.getDyeTag(DyeColor.LIGHT_BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PINK))
            .requires(ColourUtils.getDyeTag(DyeColor.WHITE))
            .requires(Items.STICK)
        );

        turtleOverlay(add, "turtle_rainbow_overlay", x -> x
            .unlockedBy("has_dye", inventoryChange(itemPredicate(ingredients.dye())))
            .requires(ColourUtils.getDyeTag(DyeColor.RED))
            .requires(ColourUtils.getDyeTag(DyeColor.ORANGE))
            .requires(ColourUtils.getDyeTag(DyeColor.YELLOW))
            .requires(ColourUtils.getDyeTag(DyeColor.GREEN))
            .requires(ColourUtils.getDyeTag(DyeColor.BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PURPLE))
            .requires(Items.STICK)
        );
    }

    private void turtleOverlay(RecipeOutput add, String overlay, Consumer<ShapelessSpecBuilder> build) {
        var overlayId = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/" + overlay);

        for (var turtleItem : turtleItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, turtleItem);

            var builder = ShapelessSpecBuilder
                .shapeless(RecipeCategory.REDSTONE, DataComponentUtil.createStack(turtleItem, ModRegistry.DataComponents.OVERLAY.get(), overlayId))
                .group(name.withSuffix("_overlay").toString())
                .unlockedBy("has_turtle", inventoryChange(turtleItem));
            build.accept(builder);
            builder
                .requires(turtleItem)
                .build(s -> new TransformShapelessRecipe(s, List.of(
                    CopyComponents.builder(turtleItem).exclude(ModRegistry.DataComponents.OVERLAY.get()).build()
                )))
                .save(add, name.withSuffix("_overlays/" + overlay));
        }
    }


    private void basicRecipes(RecipeOutput add) {
        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.CABLE.get(), 6)
            .pattern(" # ")
            .pattern("#R#")
            .pattern(" # ")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_redstone", inventoryChange(itemPredicate(ingredients.redstone())))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ingredients.redstone()), itemPredicate(ingredients.goldIngot())))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_NORMAL.get()))))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "computer_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_COMMAND.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', Blocks.COMMAND_BLOCK)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(Blocks.COMMAND_BLOCK))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_NORMAL.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.ironIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_NORMAL.get()))))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_ADVANCED.get()))))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern(" B ")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.TURTLE_NORMAL.get())
            .define('B', ingredients.goldBlock())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.TURTLE_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.TURTLE_NORMAL.get()))))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.DISK_DRIVE.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.MONITOR_NORMAL.get())
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.MONITOR_ADVANCED.get(), 4)
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
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
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_apple", inventoryChange(Items.GOLDEN_APPLE))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()))))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "pocket_computer_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.PRINTER.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#D#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('D', ingredients.dye())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.SPEAKER.get())
            .pattern("###")
            .pattern("#N#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('N', Blocks.NOTE_BLOCK)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.WIRED_MODEM.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_cable", inventoryChange(ModRegistry.Items.CABLE.get()))
            .save(add);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .requires(ModRegistry.Items.WIRED_MODEM.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_modem_full_from"));
        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.WIRED_MODEM.get())
            .requires(ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_modem_full_to"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('E', ingredients.enderPearl())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('E', Items.ENDER_EYE)
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_wireless", inventoryChange(ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get()))
            .save(add);

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.DECORATIONS, playerHead("Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c"))
            .requires(ItemTags.SKULLS)
            .requires(ModRegistry.Items.MONITOR_NORMAL.get())
            .unlockedBy("has_monitor", inventoryChange(ModRegistry.Items.MONITOR_NORMAL.get()))
            .build()
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "skull_cloudy"));

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.DECORATIONS, playerHead("dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb"))
            .requires(ItemTags.SKULLS)
            .requires(ModRegistry.Items.COMPUTER_ADVANCED.get())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_ADVANCED.get()))
            .build()
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "skull_dan200"));

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_PAGES.get())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 2)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .build(ImpostorShapelessRecipe::new)
            .save(add);

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_BOOK.get())
            .requires(ingredients.leather())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 1)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .build(ImpostorShapelessRecipe::new)
            .save(add);
    }

    private static DyeColor ofColour(Colour colour) {
        return DyeColor.byId(15 - colour.ordinal());
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(TagKey<Item> stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(itemPredicate(stack));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(ItemLike... stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(stack);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(ItemPredicate... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(items);
    }

    private static ItemPredicate itemPredicate(ItemLike item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(TagKey<Item> item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(Ingredient ingredient) {
        var json = Ingredient.CODEC_NONEMPTY.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
        if (!(json instanceof JsonObject object)) throw new IllegalStateException("Unknown ingredient " + json);

        if (object.has("item")) {
            var item = ItemStack.SIMPLE_ITEM_CODEC.parse(JsonOps.INSTANCE, object).getOrThrow();
            return itemPredicate(item.getItem());
        } else if (object.has("tag")) {
            return itemPredicate(TagKey.create(Registries.ITEM, new ResourceLocation(GsonHelper.getAsString(object, "tag"))));
        } else {
            throw new IllegalArgumentException("Unknown ingredient " + json);
        }
    }

    private static ItemStack playerHead(String name, String uuid) {
        return DataComponentUtil.createStack(Items.PLAYER_HEAD, DataComponents.PROFILE, new ResolvableProfile(new GameProfile(UUID.fromString(uuid), name)));
    }

    private static void addSpecial(RecipeOutput add, Recipe<?> recipe) {
        add.accept(RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, recipe.getSerializer()), recipe, null);
    }
}
