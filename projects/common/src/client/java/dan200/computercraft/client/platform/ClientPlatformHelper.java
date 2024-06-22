// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;

import javax.annotation.Nullable;

public interface ClientPlatformHelper extends dan200.computercraft.impl.client.ClientPlatformHelper {
    static ClientPlatformHelper get() {
        return (ClientPlatformHelper) dan200.computercraft.impl.client.ClientPlatformHelper.get();
    }

    /**
     * Render a {@link BakedModel}, using any loader-specific hooks.
     *
     * @param transform     The current matrix transformation to apply.
     * @param buffers       The current pool of render buffers.
     * @param model         The model to draw.
     * @param lightmapCoord The current packed lightmap coordinate.
     * @param overlayLight  The current overlay light.
     * @param tints         Block colour tints to apply to the model.
     */
    void renderBakedModel(PoseStack transform, MultiBufferSource buffers, BakedModel model, int lightmapCoord, int overlayLight, @Nullable int[] tints);
}
