// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.model.LecternPocketModel;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.data.client.ExtraModelsProvider;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
        generator.add(out -> new RecipeProvider(out, fullRegistries));

        var blockTags = generator.blockTags(TagProvider::blockTags);
        generator.itemTags(TagProvider::itemTags, blockTags);

        generator.add(out -> new net.minecraft.data.loot.LootTableProvider(out, Set.of(), LootTableProvider.getTables(), fullRegistries));

        generator.add(out -> new ModelProvider(out, BlockModelProvider::addBlockModels, ItemModelProvider::addItemModels));

        generator.add(out -> new LanguageProvider(out, fullRegistries));

        generator.addFromCodec("Block atlases", PackType.CLIENT_RESOURCES, "atlases", SpriteSources.FILE_CODEC, out -> {
            out.accept(ResourceLocation.withDefaultNamespace("blocks"), makeSprites(Stream.of(
                UpgradeSlot.LEFT_UPGRADE,
                UpgradeSlot.RIGHT_UPGRADE,
                LecternPrintoutModel.TEXTURE,
                LecternPocketModel.TEXTURE_NORMAL, LecternPocketModel.TEXTURE_ADVANCED,
                LecternPocketModel.TEXTURE_COLOUR, LecternPocketModel.TEXTURE_FRAME, LecternPocketModel.TEXTURE_LIGHT
            )));
            out.accept(GuiSprites.SPRITE_SHEET, makeSprites(
                // Computers
                GuiSprites.COMPUTER_NORMAL.textures(),
                GuiSprites.COMPUTER_ADVANCED.textures(),
                GuiSprites.COMPUTER_COMMAND.textures(),
                GuiSprites.COMPUTER_COLOUR.textures()
            ));
        });

        generator.add(pack -> new ExtraModelsProvider(pack, fullRegistries) {
            @Override
            public Stream<ResourceLocation> getModels(HolderLookup.Provider registries) {
                return registries.lookupOrThrow(TurtleOverlay.REGISTRY).listElements().map(x -> x.value().model());
            }
        });
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static List<SpriteSource> makeSprites(final Stream<ResourceLocation>... files) {
        return Arrays.stream(files).flatMap(Function.identity()).<SpriteSource>map(x -> new SingleFile(x, Optional.empty())).toList();
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
