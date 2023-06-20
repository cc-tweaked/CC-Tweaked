// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import dan200.computercraft.data.DataProviders;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.Optional;

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
        });
    }
}
