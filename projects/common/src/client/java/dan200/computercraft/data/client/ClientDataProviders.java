// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.data.DataProviders;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A version of {@link DataProviders} which relies on client-side classes.
 * <p>
 * This is called from {@link DataProviders#add(DataProviders.GeneratorSink)}.
 */
public final class ClientDataProviders {
    private ClientDataProviders() {
    }

    public static void add(DataProviders.GeneratorSink generator, CompletableFuture<HolderLookup.Provider> registries) {
        generator.addFromCodec("Block atlases", PackType.CLIENT_RESOURCES, "atlases", SpriteSources.FILE_CODEC, out -> {
            out.accept(ResourceLocation.withDefaultNamespace("blocks"), makeSprites(Stream.of(
                // Upgrade slot backgrounds
                UpgradeSlot.LEFT_UPGRADE,
                UpgradeSlot.RIGHT_UPGRADE,
                // Texture for lectern printouts
                LecternPrintoutModel.TEXTURE
            )));

            out.accept(GuiSprites.SPRITE_SHEET, makeSprites(
                // Computers
                GuiSprites.COMPUTER_NORMAL.textures(),
                GuiSprites.COMPUTER_ADVANCED.textures(),
                GuiSprites.COMPUTER_COMMAND.textures(),
                GuiSprites.COMPUTER_COLOUR.textures()
            ));
        });

        generator.add(pack -> new ExtraModelsProvider(pack, registries) {
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
}
