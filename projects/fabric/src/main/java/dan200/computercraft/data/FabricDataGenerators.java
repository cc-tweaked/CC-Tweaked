// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = new PlatformGeneratorsImpl(generator.createPack());
        DataProviders.add(pack);
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
            return addWithFabricOutput(factory::create);
        }

        @Override
        public <T extends DataProvider> T add(BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> factory) {
            return addWithRegistries(factory::apply);
        }

        @Override
        public <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output) {
            addWithRegistries((out, registries) -> {
                var ourType = switch (type) {
                    case SERVER_DATA -> PackOutput.Target.DATA_PACK;
                    case CLIENT_RESOURCES -> PackOutput.Target.RESOURCE_PACK;
                };
                return new FabricCodecDataProvider<T>(out, registries, ourType, directory, codec) {
                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    protected void configure(BiConsumer<ResourceLocation, T> provider, HolderLookup.Provider registries) {
                        output.accept(provider);
                    }
                };
            });
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
        public CompletableFuture<RegistrySetBuilder.PatchedRegistries> createPatchedRegistries(CompletableFuture<HolderLookup.Provider> registries, RegistrySetBuilder patch) {
            return registries.thenApply(oldRegistries -> {
                var factory = new Cloner.Factory();
                DynamicRegistries.getDynamicRegistries().forEach(registryData -> registryData.runWithArguments(factory::addCodec));
                return patch.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), new DynamicRegistryLookup(oldRegistries), factory);
            });
        }

        /**
         * A {@link HolderLookup.Provider} implementation that adds any Fabric dynamic registry, if missing.
         *
         * @param parent The parent registry.
         */
        private record DynamicRegistryLookup(HolderLookup.Provider parent) implements HolderLookup.Provider {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return Stream.concat(
                    parent.listRegistries(),
                    DynamicRegistries.getDynamicRegistries().stream().map(RegistryDataLoader.RegistryData::key)
                ).distinct();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                return parent.lookup(registryKey).or(() -> Optional.of(new EmptyRegistry<>(registryKey)));
            }
        }

        private record EmptyRegistry<T>(
            ResourceKey<? extends Registry<? extends T>> key
        ) implements HolderLookup.RegistryLookup<T> {
            @Override
            public Lifecycle registryLifecycle() {
                return Lifecycle.stable();
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return Stream.empty();
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> resourceKey) {
                return Optional.empty();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey) {
                return Optional.empty();
            }
        }
    }
}
