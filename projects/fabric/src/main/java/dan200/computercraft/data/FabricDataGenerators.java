// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = new PlatformGeneratorsImpl(generator.createPack());
        DataProviders.add(pack);
        pack.addWithRegistries((out, reg) -> addName("Conventional Tags", new MoreConventionalTagsProvider(out, reg)));
    }

    private record PlatformGeneratorsImpl(FabricDataGenerator.Pack generator) implements DataProviders.GeneratorSink {
        public <T extends DataProvider> T addWithFabricOutput(FabricDataGenerator.Pack.Factory<T> factory) {
            return generator.addProvider((FabricDataOutput p) -> new PrettyDataProvider<>(factory.create(p))).provider();
        }

        public <T extends DataProvider> T addWithRegistries(FabricDataGenerator.Pack.RegistryDependentFactory<T> factory) {
            return generator.addProvider((r, p) -> new PrettyDataProvider<>(factory.create(r, p))).provider();
        }

        @Override
        public <T extends DataProvider> T add(DataProvider.Factory<T> factory) {
            return generator.addProvider((PackOutput p) -> new PrettyDataProvider<>(factory.create(p))).provider();
        }

        @Override
        public <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output) {
            addWithFabricOutput((FabricDataOutput out) -> {
                var ourType = switch (type) {
                    case SERVER_DATA -> PackOutput.Target.DATA_PACK;
                    case CLIENT_RESOURCES -> PackOutput.Target.RESOURCE_PACK;
                };
                return new FabricCodecDataProvider<T>(out, ourType, directory, codec) {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    protected void configure(BiConsumer<ResourceLocation, T> provider) {
                        output.accept(provider);
                    }
                };
            });
        }

        @Override
        public void lootTable(List<LootTableProvider.SubProviderEntry> tables) {
            for (var table : tables) {
                addWithFabricOutput((FabricDataOutput out) -> new SimpleFabricLootTableProvider(out, table.paramSet()) {
                    @Override
                    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> exporter) {
                        table.provider().get().generate(exporter);
                    }
                });
            }
        }

        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return addWithRegistries((out, registries) -> new FabricTagProvider.BlockTagProvider(out, registries) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    tags.accept(x -> new TagProvider.TagAppender<>(BuiltInRegistries.BLOCK, getOrCreateRawBuilder(x)));
                }
            });
        }

        @Override
        public TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks) {
            return addWithRegistries((out, registries) -> new FabricTagProvider.ItemTagProvider(out, registries, (FabricTagProvider.BlockTagProvider) blocks) {
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
            addWithFabricOutput((FabricDataOutput out) -> new FabricModelProvider(out) {
                @Override
                public void generateBlockStateModels(BlockModelGenerators generator) {
                    blocks.accept(generator);
                }

                @Override
                public void generateItemModels(ItemModelGenerators generator) {
                    items.accept(generator);
                }
            });
        }
    }


    /**
     * Add a name to a data provider to disambiguate them.
     *
     * @param suffix   The suffix to add.
     * @param provider The data provider to wrap.
     * @return The wrapped data provider.
     */
    private static DataProvider addName(String suffix, DataProvider provider) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput output) {
                return provider.run(output);
            }

            @Override
            public String getName() {
                return provider.getName() + " - " + suffix;
            }
        };
    }
}
