// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ForgeDataProviders {
    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        DataProviders.add(new GeneratorSinkImpl(generator.getVanillaPack(true), event.getLookupProvider()));
    }

    private record GeneratorSinkImpl(
        DataGenerator.PackGenerator generator,
        CompletableFuture<HolderLookup.Provider> registries
    ) implements DataProviders.GeneratorSink {
        @Override
        public <T extends DataProvider> T add(DataProvider.Factory<T> factory) {
            return generator.addProvider(p -> new PrettyDataProvider<>(factory.create(p))).provider();
        }

        @Override
        public <T> void addFromCodec(String name, PackOutput.Target target, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output) {
            add(out -> new JsonCodecProvider<T>(out, target, directory, codec, registries, ComputerCraftAPI.MOD_ID) {
                @Override
                protected void gather() {
                    output.accept(this::unconditional);
                }
            });
        }

        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return add(out -> new BlockTagsProvider(out, registries, ComputerCraftAPI.MOD_ID) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    tags.accept(this::tag);
                }
            });
        }

        @Override
        public TagsProvider<Item> itemTags(Consumer<TagProvider.TagConsumer<Item>> tags) {
            return add(out -> new ItemTagsProvider(out, registries, ComputerCraftAPI.MOD_ID) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    tags.accept(this::tag);
                }
            });
        }

        @Override
        public void registries(CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries) {
            add(out -> new DatapackBuiltinEntriesProvider(out, registries, Set.of(ComputerCraftAPI.MOD_ID, "minecraft")));
        }

        @Override
        public void addModels(Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
            add(out -> new ModelProvider(out, ComputerCraftAPI.MOD_ID) {
                @Override
                protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
                    blocks.accept(blockModels);
                    items.accept(itemModels);
                }
            });
        }
    }
}
