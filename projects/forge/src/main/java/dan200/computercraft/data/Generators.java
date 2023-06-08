// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Generators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        DataProviders.add(new GeneratorFactoryImpl(generator.getVanillaPack(true), event.getExistingFileHelper(), event.getLookupProvider()));

        generator.<LootModifierProvider>addProvider(event.includeServer(), LootModifierProvider::new);
    }

    private record GeneratorFactoryImpl(
        DataGenerator.PackGenerator generator,
        ExistingFileHelper existingFiles,
        CompletableFuture<HolderLookup.Provider> registries
    ) implements DataProviders.GeneratorSink {
        @Override
        public <T extends DataProvider> T add(DataProvider.Factory<T> factory) {
            return generator.addProvider(factory);
        }

        @Override
        public void lootTable(List<LootTableProvider.SubProviderEntry> tables) {
            add(out -> new LootTableProvider(out, Set.of(), tables));
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

        @Override
        public void models(Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
            add(out -> new ModelProvider(out, blocks, items));
        }
    }
}
