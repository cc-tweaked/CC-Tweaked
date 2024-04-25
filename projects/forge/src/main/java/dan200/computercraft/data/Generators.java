// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class Generators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        DataProviders.add(new GeneratorFactoryImpl(generator.getVanillaPack(true), event.getExistingFileHelper(), event.getLookupProvider()));
    }

    private record GeneratorFactoryImpl(
        DataGenerator.PackGenerator generator,
        ExistingFileHelper existingFiles,
        CompletableFuture<HolderLookup.Provider> registries
    ) implements DataProviders.GeneratorSink {
        @Override
        public <T extends DataProvider> T add(DataProvider.Factory<T> factory) {
            return generator.addProvider(p -> new PrettyDataProvider<>(factory.create(p))).provider();
        }

        @Override
        public <T extends DataProvider> T add(BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> factory) {
            return generator.addProvider(p -> factory.apply(p, registries));
        }

        @Override
        public <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output) {
            add(out -> {
                var target = switch (type) {
                    case SERVER_DATA -> PackOutput.Target.DATA_PACK;
                    case CLIENT_RESOURCES -> PackOutput.Target.RESOURCE_PACK;
                };
                return new JsonCodecProvider<T>(out, target, directory, type, codec, registries, ComputerCraftAPI.MOD_ID, existingFiles) {
                    @Override
                    protected void gather() {
                        output.accept(this::unconditional);
                    }
                };
            });
        }

        @Override
        public void lootTable(List<LootTableProvider.SubProviderEntry> tables) {
            add((out, registries) -> new LootTableProvider(out, Set.of(), tables, registries));
        }

        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return add(out -> new BlockTagsProvider(out, registries, ComputerCraftAPI.MOD_ID, existingFiles) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    tags.accept(x -> new TagProvider.TagAppender<>(BuiltInRegistries.BLOCK, getOrCreateRawBuilder(x)));
                }
            });
        }

        @Override
        public TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks) {
            return add(out -> new ItemTagsProvider(out, registries, blocks.contentsGetter(), ComputerCraftAPI.MOD_ID, existingFiles) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    var self = this;
                    tags.accept(new TagProvider.ItemTagConsumer() {
                        @Override
                        public TagProvider.TagAppender<Item> tag(TagKey<Item> tag) {
                            return new TagProvider.TagAppender<>(BuiltInRegistries.ITEM, getOrCreateRawBuilder(tag));
                        }

                        @Override
                        public void copy(TagKey<Block> block, TagKey<Item> item) {
                            self.copy(block, item);
                        }
                    });
                }
            });
        }

        @Override
        public void models(Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
            add(out -> new ModelProvider(out, blocks, items));
        }
    }
}
