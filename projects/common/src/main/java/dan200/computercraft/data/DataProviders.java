// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.shared.ModRegistry;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * All data providers for ComputerCraft. We require a mod-loader abstraction {@link GeneratorSink} (instead of
 * {@link PackOutput})to handle the slight differences between how Forge and Fabric expose Minecraft's data providers.
 */
public final class DataProviders {
    private DataProviders() {
    }

    public static void add(GeneratorSink generator) {
        var turtleUpgrades = generator.add(TurtleUpgradeProvider::new);
        var pocketUpgrades = generator.add(PocketUpgradeProvider::new);

        generator.add((out, registries) -> {
            var builder = new RegistrySetBuilder();
            builder.add(ModRegistry.TURTLE_UPGRADE, bs -> turtleUpgrades.getGeneratedUpgrades().forEach(bs::register));
            builder.add(ModRegistry.POCKET_UPGRADE, bs -> pocketUpgrades.getGeneratedUpgrades().forEach(bs::register));

            return new RecipeProvider(out, generator.createPatchedRegistries(registries, builder).thenApply(RegistrySetBuilder.PatchedRegistries::full));
        });

        var blockTags = generator.blockTags(TagProvider::blockTags);
        generator.itemTags(TagProvider::itemTags, blockTags);

        generator.add((out, registries) -> new net.minecraft.data.loot.LootTableProvider(out, Set.of(), LootTableProvider.getTables(), registries));

        generator.add(out -> new ModelProvider(out, BlockModelProvider::addBlockModels, ItemModelProvider::addItemModels));

        generator.add(out -> new LanguageProvider(out, turtleUpgrades, pocketUpgrades));

        // Unfortunately we rely on some client-side classes in this code. We just load in the client side data provider
        // and invoke that.
        try {
            Class.forName("dan200.computercraft.data.client.ClientDataProviders")
                .getMethod("add", GeneratorSink.class).invoke(null, generator);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public interface GeneratorSink {
        <T extends DataProvider> T add(DataProvider.Factory<T> factory);

        <T extends DataProvider> T add(BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> factory);

        <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output);

        TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags);

        TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks);

        /**
         * Extend our registries with additional entries.
         *
         * @param registries The existing registries.
         * @param patch      The new registries to apply.
         * @return The built registries.
         */
        default CompletableFuture<RegistrySetBuilder.PatchedRegistries> createPatchedRegistries(
            CompletableFuture<HolderLookup.Provider> registries, RegistrySetBuilder patch
        ) {
            return RegistryPatchGenerator.createLookup(registries, patch);
        }
    }
}
