// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.libmultipart;

import alexiil.mc.lib.multipart.api.render.PartStaticModelRegisterEvent;
import dan200.computercraft.shared.integration.libmultipart.BlockStateModelKey;
import dan200.computercraft.shared.integration.libmultipart.LibMultiPartIntegration;
import net.minecraft.client.Minecraft;

/**
 * Client-side support for LibMultiPart.
 *
 * @see LibMultiPartIntegration
 */
public class LibMultiPartIntegrationClient {
    public static void init() {
        PartStaticModelRegisterEvent.EVENT.register(renderer -> {
            var baker = Minecraft.getInstance().getBlockRenderer();
            renderer.register(BlockStateModelKey.class, (key, ctx) ->
                ctx.bakedModelConsumer().accept(baker.getBlockModel(key.state()), key.state()));
        });
    }
}
