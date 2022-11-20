/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * All data providers for ComputerCraft. We require a mod-loader abstraction {@link DataProviders.GeneratorFactory} to
 * handle the slight differences between how Forge and Fabric expose Minecraft's data providers.
 */
public final class DataProviders {
    private DataProviders() {
    }

    public static void add(DataGenerator generator, GeneratorFactory generators, boolean includeServer, boolean includeClient) {
        var turtleUpgrades = new TurtleUpgradeProvider(generator);
        var pocketUpgrades = new PocketUpgradeProvider(generator);

        generator.addProvider(includeServer, turtleUpgrades);
        generator.addProvider(includeServer, pocketUpgrades);
        generator.addProvider(includeServer, generators.recipes(new RecipeProvider(turtleUpgrades, pocketUpgrades)::addRecipes));

        var blockTags = generators.blockTags(TagProvider::blockTags);
        generator.addProvider(includeServer, blockTags);
        generator.addProvider(includeServer, generators.itemTags(TagProvider::itemTags, blockTags));

        for (var provider : generators.lootTable(LootTableProvider.getTables())) {
            generator.addProvider(includeServer, provider);
        }

        generator.addProvider(includeClient, generators.models(BlockModelProvider::addBlockModels, ItemModelProvider::addItemModels));

        generator.addProvider(includeServer, new LanguageProvider(generator, turtleUpgrades, pocketUpgrades));
    }

    interface GeneratorFactory {
        DataProvider recipes(Consumer<Consumer<FinishedRecipe>> recipes);

        List<DataProvider> lootTable(List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> tables);

        TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags);

        TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks);

        DataProvider models(Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items);
    }
}
