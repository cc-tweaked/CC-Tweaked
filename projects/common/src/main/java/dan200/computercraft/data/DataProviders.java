// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * All data providers for ComputerCraft. We require a mod-loader abstraction {@link GeneratorSink} (instead of
 * {@link PackOutput})to handle the slight differences between how Forge and Fabric expose Minecraft's data providers.
 */
public final class DataProviders {
    private DataProviders() {
    }

    public static void add(GeneratorSink generator) {
        var fullRegistryPatch = RegistryPatchGenerator.createLookup(
            generator.registries(),
            Util.make(new RegistrySetBuilder(), builder -> {
                builder.add(ITurtleUpgrade.REGISTRY, TurtleUpgradeProvider::addUpgrades);
                builder.add(IPocketUpgrade.REGISTRY, PocketUpgradeProvider::addUpgrades);
                builder.add(TurtleOverlay.REGISTRY, TurtleOverlays::register);
            }));
        var fullRegistries = fullRegistryPatch.thenApply(RegistrySetBuilder.PatchedRegistries::full);

        generator.registries(fullRegistryPatch);
        generator.add(out -> new RecipeProvider.Runner(out, fullRegistries));

        var blockTags = generator.blockTags(TagProvider::blockTags);
        generator.itemTags(TagProvider::itemTags, blockTags);

        generator.add(out -> new net.minecraft.data.loot.LootTableProvider(out, Set.of(), LootTableProvider.getTables(), fullRegistries));

        generator.add(out -> new ModelProvider(out, BlockModelProvider::addBlockModels, ItemModelProvider::addItemModels));

        generator.add(out -> new LanguageProvider(out, fullRegistries));

        // Unfortunately we rely on some client-side classes in this code. We just load in the client side data provider
        // and invoke that.
        try {
            Class.forName("dan200.computercraft.data.client.ClientDataProviders")
                .getMethod("add", GeneratorSink.class, CompletableFuture.class)
                .invoke(null, generator, fullRegistries);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public interface GeneratorSink {
        CompletableFuture<HolderLookup.Provider> registries();

        <T extends DataProvider> T add(DataProvider.Factory<T> factory);

        <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output);

        TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags);

        TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks);

        /**
         * Build new dynamic registries and save them to a pack.
         *
         * @param registries The patched registries to write.
         */
        void registries(CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries);
    }
}
