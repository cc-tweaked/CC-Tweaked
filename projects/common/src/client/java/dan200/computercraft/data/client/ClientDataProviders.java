// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.data.DataProviders;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A version of {@link DataProviders} which relies on client-side classes.
 * <p>
 * This is called from {@link DataProviders#add(DataProviders.GeneratorSink)}.
 */
public final class ClientDataProviders {
    private ClientDataProviders() {
    }

    public static void add(DataProviders.GeneratorSink generator) {
        generator.addFromCodec("Block atlases", PackType.CLIENT_RESOURCES, "atlases", SpriteSources.FILE_CODEC, out -> {
            out.accept(new ResourceLocation("blocks"), List.of(
                new SingleFile(UpgradeSlot.LEFT_UPGRADE, Optional.empty()),
                new SingleFile(UpgradeSlot.RIGHT_UPGRADE, Optional.empty())
            ));
            out.accept(GuiSprites.SPRITE_SHEET, Stream.of(
                // Buttons
                GuiSprites.TURNED_OFF.textures(),
                GuiSprites.TURNED_ON.textures(),
                GuiSprites.TERMINATE.textures(),
                // Computers
                GuiSprites.COMPUTER_NORMAL.textures(),
                GuiSprites.COMPUTER_ADVANCED.textures(),
                GuiSprites.COMPUTER_COMMAND.textures(),
                GuiSprites.COMPUTER_COLOUR.textures()
            ).flatMap(x -> x).<SpriteSource>map(x -> new SingleFile(x, Optional.empty())).toList());
        });
    }
}
