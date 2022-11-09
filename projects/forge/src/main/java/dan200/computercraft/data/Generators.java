/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.mojang.datafixers.util.Pair;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Generators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        DataProviders.add(
            generator, new GeneratorFactoryImpl(generator, event.getExistingFileHelper()),
            event.includeServer(), event.includeClient()
        );
    }

    private record GeneratorFactoryImpl(
        DataGenerator generator, ExistingFileHelper existingFiles
    ) implements DataProviders.GeneratorFactory {
        @Override
        public DataProvider recipes(Consumer<Consumer<FinishedRecipe>> recipes) {
            return new RecipeProvider(generator) {
                @Override
                protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
                    recipes.accept(consumer);
                }
            };
        }

        @Override
        public List<DataProvider> lootTable(List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> tables) {
            return List.of(new LootTableProvider(generator) {
                @Override
                protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
                    return tables;
                }

                @Override
                protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext tracker) {
                    map.forEach((id, table) -> LootTables.validate(tracker, id, table));
                }
            });
        }

        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return new BlockTagsProvider(generator, ComputerCraftAPI.MOD_ID, existingFiles) {
                @Override
                protected void addTags() {
                    tags.accept(this::tag);
                }
            };
        }

        @Override
        public TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks) {
            return new ItemTagsProvider(generator, (BlockTagsProvider) blocks, ComputerCraftAPI.MOD_ID, existingFiles) {
                @Override
                protected void addTags() {
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
            return new ModelProvider(generator, blocks, items);
        }
    }
}
