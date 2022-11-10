/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.data.DataProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        DataProviders.add(generator, new PlatformGeneratorsImpl(generator), true, true);

        generator.addProvider(new MoreConventionalTagsProvider(generator));
    }

    private record PlatformGeneratorsImpl(FabricDataGenerator generator) implements DataProviders.GeneratorFactory {
        @Override
        public DataProvider recipes(Consumer<Consumer<FinishedRecipe>> recipes) {
            return new FabricRecipeProvider(generator) {
                @Override
                protected void generateRecipes(Consumer<FinishedRecipe> exporter) {
                    recipes.accept(exporter);
                }
            };
        }

        @Override
        public List<DataProvider> lootTable(List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> tables) {
            return tables.stream().<DataProvider>map(table -> new SimpleFabricLootTableProvider(generator, table.getSecond()) {
                @Override
                public void accept(BiConsumer<ResourceLocation, LootTable.Builder> exporter) {
                    table.getFirst().get().accept(exporter);
                }
            }).toList();
        }


        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return new FabricTagProvider.BlockTagProvider(generator) {
                @Override
                protected void generateTags() {
                    tags.accept(this::tag);
                }
            };
        }

        @Override
        public TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks) {
            return new FabricTagProvider.ItemTagProvider(generator, (FabricTagProvider.BlockTagProvider) blocks) {
                @Override
                protected void generateTags() {
                    var self = this;
                    tags.accept(new TagProvider.ItemTagConsumer() {
                        @Override
                        public TagAppender<Item> tag(TagKey<Item> tag) {
                            return self.tag(tag);
                        }

                        @Override
                        public void copy(TagKey<Block> block, TagKey<Item> item) {
                            self.copy(block, item);
                        }
                    });
                }
            };
        }

        @Override
        public DataProvider models(Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
            return new FabricModelProvider(generator) {
                @Override
                public void generateBlockStateModels(BlockModelGenerators generator) {
                    blocks.accept(generator);
                }

                @Override
                public void generateItemModels(ItemModelGenerators generator) {
                    items.accept(generator);
                }
            };
        }
    }
}
