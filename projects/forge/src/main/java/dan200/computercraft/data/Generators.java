// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
        public <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output) {
            add(out -> {
                Map<ResourceLocation, T> map = new HashMap<>();
                output.accept(map::put);
                return new JsonCodecProvider<>(out, existingFiles, ComputerCraftAPI.MOD_ID, JsonOps.INSTANCE, type, directory, codec, map);
            });
        }

        @Override
        public TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags) {
            return add(out -> new BlockTagsProvider(out, registries, ComputerCraftAPI.MOD_ID, existingFiles) {
                @Override
                protected void addTags(HolderLookup.Provider registries) {
                    tags.accept(x -> new TagProvider.TagAppender<>(RegistryWrappers.BLOCKS, getOrCreateRawBuilder(x)));
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
                            return new TagProvider.TagAppender<>(RegistryWrappers.ITEMS, getOrCreateRawBuilder(tag));
                        }

                        @Override
                        public void copy(TagKey<Block> block, TagKey<Item> item) {
                            self.copy(block, item);
                        }
                    });
                }
            });
        }
    }
}
