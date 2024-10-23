// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.data.recipe.ShapedSpecBuilder;
import dan200.computercraft.data.recipe.ShapelessSpecBuilder;
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
import dan200.computercraft.shared.recipe.TransformShapedRecipe;
import dan200.computercraft.shared.recipe.TransformShapelessRecipe;
import dan200.computercraft.shared.recipe.function.CopyComponents;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

final class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    private final RecipeIngredients ingredients;
    private final HolderGetter<Item> items;

    RecipeProvider(HolderLookup.Provider registries, RecipeOutput recipeOutput) {
        super(registries, recipeOutput);
        this.items = registries.lookupOrThrow(Registries.ITEM);
        ingredients = PlatformHelper.get().getRecipeIngredients();
    }

    @Override
    public void buildRecipes() {
        basicRecipes();
        diskColours();
        pocketUpgrades();
        turtleUpgrades();
        turtleOverlays();

        special(new ColourableRecipe(CraftingBookCategory.MISC));
        special(new ClearColourRecipe(CraftingBookCategory.MISC));
        special(new TurtleUpgradeRecipe(CraftingBookCategory.MISC));
        special(new PocketComputerUpgradeRecipe(CraftingBookCategory.MISC));
    }

    /**
     * Register a disk recipe.
     */
    private void diskColours() {
        customShapeless(RecipeCategory.REDSTONE, ModRegistry.Items.DISK.get())
            .requires(ingredients.redstone())
            .requires(Items.PAPER)
            .group("computercraft:disk")
            .unlockedBy("has_drive", has(ModRegistry.Items.DISK_DRIVE.get()))
            .build(d -> new DiskRecipe(d.properties(), d.ingredients()))
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "disk"));
    }

    private static List<TurtleItem> turtleItems() {
        return List.of(ModRegistry.Items.TURTLE_NORMAL.get(), ModRegistry.Items.TURTLE_ADVANCED.get());
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     */
    private void turtleUpgrades() {
        for (var turtleItem : turtleItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, turtleItem);

            registries.lookupOrThrow(ITurtleUpgrade.REGISTRY).listElements().forEach(upgradeHolder -> {
                var upgrade = upgradeHolder.value();
                customShaped(RecipeCategory.REDSTONE, DataComponentUtil.createStack(turtleItem, ModRegistry.DataComponents.RIGHT_TURTLE_UPGRADE.get(), UpgradeData.ofDefault(upgradeHolder)))
                    .group(name.toString())
                    .pattern("#T")
                    .define('T', turtleItem)
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", has(turtleItem, upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        output,
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
     */
    private void pocketUpgrades() {
        for (var pocket : pocketComputerItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, pocket).withPath(x -> x.replace("pocket_computer_", "pocket_"));

            registries.lookupOrThrow(IPocketUpgrade.REGISTRY).listElements().forEach(upgradeHolder -> {
                var upgrade = upgradeHolder.value();
                customShaped(RecipeCategory.REDSTONE, DataComponentUtil.createStack(pocket, ModRegistry.DataComponents.POCKET_UPGRADE.get(), UpgradeData.ofDefault(upgradeHolder)))
                    .group(name.toString())
                    .pattern("#")
                    .pattern("P")
                    .define('P', pocket)
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", has(pocket, upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        output,
                        name.withSuffix(String.format("/%s/%s", upgradeHolder.key().location().getNamespace(), upgradeHolder.key().location().getPath()))
                    );
            });
        }
    }

    private void turtleOverlays() {
        turtleOverlay(TurtleOverlays.TRANS_FLAG, x -> x
            .unlockedBy("has_dye", has(ingredients.dye()))
            .requires(ColourUtils.getDyeTag(DyeColor.LIGHT_BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PINK))
            .requires(ColourUtils.getDyeTag(DyeColor.WHITE))
            .requires(Items.STICK)
        );

        turtleOverlay(TurtleOverlays.RAINBOW_FLAG, x -> x
            .unlockedBy("has_dye", has(ingredients.dye()))
            .requires(ColourUtils.getDyeTag(DyeColor.RED))
            .requires(ColourUtils.getDyeTag(DyeColor.ORANGE))
            .requires(ColourUtils.getDyeTag(DyeColor.YELLOW))
            .requires(ColourUtils.getDyeTag(DyeColor.GREEN))
            .requires(ColourUtils.getDyeTag(DyeColor.BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PURPLE))
            .requires(Items.STICK)
        );
    }

    private void turtleOverlay(ResourceKey<TurtleOverlay> overlay, Consumer<ShapelessSpecBuilder> build) {
        var holder = registries.lookupOrThrow(overlay.registryKey()).getOrThrow(overlay);

        for (var turtleItem : turtleItems()) {
            var name = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, turtleItem);

            var builder = customShapeless(RecipeCategory.REDSTONE, DataComponentUtil.createStack(turtleItem, ModRegistry.DataComponents.OVERLAY.get(), holder))
                .group(name.withSuffix("_overlay").toString())
                .unlockedBy("has_turtle", has(turtleItem));
            build.accept(builder);
            builder
                .requires(turtleItem)
                .build(s -> new TransformShapelessRecipe(s, List.of(
                    CopyComponents.builder(turtleItem).exclude(ModRegistry.DataComponents.OVERLAY.get()).build()
                )))
                .save(output, name.withSuffix("_overlays/" + overlay.location().getPath()));
        }
    }


    private void basicRecipes() {
        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.CABLE.get(), 6)
            .pattern(" # ")
            .pattern("#R#")
            .pattern(" # ")
            .define('#', Items.STONE)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", has(COMPUTER))
            .unlockedBy("has_modem", has(WIRED_MODEM))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', Items.STONE)
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_redstone", has(ingredients.redstone()))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryTrigger(itemPredicate(ingredients.redstone()), itemPredicate(ingredients.goldIngot())))
            .save(output);

        customShaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryTrigger(itemPredicate(ModRegistry.Items.COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_NORMAL.get()))))
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "computer_advanced_upgrade"));

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_COMMAND.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', Items.COMMAND_BLOCK)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", has(Items.COMMAND_BLOCK))
            .save(output);

        customShaped(RecipeCategory.REDSTONE, ModRegistry.Items.TURTLE_NORMAL.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.ironIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", has(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_NORMAL.get()))))
            .save(output);

        customShaped(RecipeCategory.REDSTONE, ModRegistry.Items.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", has(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.COMPUTER_ADVANCED.get()))))
            .save(output);

        customShaped(RecipeCategory.REDSTONE, ModRegistry.Items.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern(" B ")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.TURTLE_NORMAL.get())
            .define('B', ingredients.goldBlock())
            .unlockedBy("has_components", inventoryTrigger(itemPredicate(ModRegistry.Items.TURTLE_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.TURTLE_NORMAL.get()))))
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "turtle_advanced_upgrade"));

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.DISK_DRIVE.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#R#")
            .define('#', Items.STONE)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.MONITOR_NORMAL.get())
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', Items.STONE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.MONITOR_ADVANCED.get(), 4)
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', Items.STONE)
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", has(COMPUTER))
            .unlockedBy("has_apple", has(Items.GOLDEN_APPLE))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", has(COMPUTER))
            .unlockedBy("has_apple", has(Items.GOLDEN_APPLE))
            .save(output);

        customShaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryTrigger(itemPredicate(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(x -> new TransformShapedRecipe(x, List.of(new CopyComponents(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()))))
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_advanced_upgrade"));

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTER.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#D#")
            .define('#', Items.STONE)
            .define('R', ingredients.redstone())
            .define('D', ingredients.dye())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.SPEAKER.get())
            .pattern("###")
            .pattern("#N#")
            .pattern("#R#")
            .define('#', Items.STONE)
            .define('N', Items.NOTE_BLOCK)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.WIRED_MODEM.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("###")
            .define('#', Items.STONE)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", has(COMPUTER))
            .unlockedBy("has_cable", has(ModRegistry.Items.CABLE.get()))
            .save(output);

        oneToOneConversionRecipe(ModRegistry.Items.WIRED_MODEM.get(), ModRegistry.Items.WIRED_MODEM_FULL.get(), null);
        oneToOneConversionRecipe(ModRegistry.Items.WIRED_MODEM_FULL.get(), ModRegistry.Items.WIRED_MODEM.get(), null);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.WIRELESS_MODEM_NORMAL.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', Items.STONE)
            .define('E', ingredients.enderPearl())
            .unlockedBy("has_computer", has(COMPUTER))
            .save(output);

        shaped(RecipeCategory.REDSTONE, ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('E', Items.ENDER_EYE)
            .unlockedBy("has_computer", has(COMPUTER))
            .unlockedBy("has_wireless", has(ModRegistry.Items.WIRELESS_MODEM_NORMAL.get()))
            .save(output);

        customShapeless(RecipeCategory.DECORATIONS, playerHead("Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c"))
            .requires(ItemTags.SKULLS)
            .requires(ModRegistry.Items.MONITOR_NORMAL.get())
            .unlockedBy("has_monitor", has(ModRegistry.Items.MONITOR_NORMAL.get()))
            .build()
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "skull_cloudy"));

        customShapeless(RecipeCategory.DECORATIONS, playerHead("dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb"))
            .requires(ItemTags.SKULLS)
            .requires(ModRegistry.Items.COMPUTER_ADVANCED.get())
            .unlockedBy("has_computer", has(ModRegistry.Items.COMPUTER_ADVANCED.get()))
            .build()
            .save(output, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "skull_dan200"));

        var pages = Ingredient.of(
            ModRegistry.Items.PRINTED_PAGE.get(),
            ModRegistry.Items.PRINTED_PAGES.get(),
            Items.PAPER
        );

        customShapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_PAGES.get())
            .requires(ingredients.string())
            .unlockedBy("has_printer", has(ModRegistry.Items.PRINTER.get()))
            .build(x -> new PrintoutRecipe(x, pages, 2))
            .save(output);

        customShapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_BOOK.get())
            .requires(ingredients.leather())
            .requires(ingredients.string())
            .unlockedBy("has_printer", has(ModRegistry.Items.PRINTER.get()))
            .build(x -> new PrintoutRecipe(x, pages, 1))
            .save(output);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(items);
    }

    private ItemPredicate itemPredicate(ItemLike item) {
        return ItemPredicate.Builder.item().of(items, item).build();
    }

    private ItemPredicate itemPredicate(TagKey<Item> item) {
        return ItemPredicate.Builder.item().of(items, item).build();
    }

    private static ItemStack playerHead(String name, String uuid) {
        return DataComponentUtil.createStack(Items.PLAYER_HEAD, DataComponents.PROFILE, new ResolvableProfile(new GameProfile(UUID.fromString(uuid), name)));
    }

    private ShapedSpecBuilder customShaped(RecipeCategory category, ItemStack result) {
        return new ShapedSpecBuilder(items, category, result);
    }

    private ShapedSpecBuilder customShaped(RecipeCategory category, ItemLike result) {
        return new ShapedSpecBuilder(items, category, new ItemStack(result));
    }

    private ShapelessSpecBuilder customShapeless(RecipeCategory category, ItemStack result) {
        return new ShapelessSpecBuilder(items, category, result);
    }

    private ShapelessSpecBuilder customShapeless(RecipeCategory category, ItemLike result) {
        return new ShapelessSpecBuilder(items, category, new ItemStack(result));
    }

    private void special(Recipe<?> recipe) {
        var key = RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, recipe.getSerializer());
        output.accept(recipeKey(key), recipe, null);
    }

    public static ResourceKey<Recipe<?>> recipeKey(ResourceLocation key) {
        return ResourceKey.create(Registries.RECIPE, key);
    }

    static class Runner extends net.minecraft.data.recipes.RecipeProvider.Runner {
        protected Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new RecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Recipes";
        }
    }
}
