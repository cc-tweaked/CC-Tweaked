// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.model.FoiledModel;
import dan200.computercraft.client.render.ModelRenderer;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.server.ServerNetworkContext;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

@AutoService(dan200.computercraft.impl.client.ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    private static final RandomSource random = RandomSource.create(0);

    @Override
    public void sendToServer(NetworkMessage<ServerNetworkContext> message) {
        Minecraft.getInstance().player.connection.send(NetworkHandler.encodeServer(message));
    }

    @Override
    public BakedModel getModel(ModelManager manager, ResourceLocation location) {
        var model = manager.getModel(location);
        return model == null ? manager.getMissingModel() : model;
    }

    @Override
    public BakedModel createdFoiledModel(BakedModel model) {
        return new FoiledModel(model);
    }

    @Override
    public void renderBakedModel(PoseStack transform, MultiBufferSource buffers, BakedModel model, int lightmapCoord, int overlayLight, @Nullable int[] tints) {
        // Unfortunately we can't call Fabric's emitItemQuads here, as there's no way to obtain a RenderContext via the
        // API. Instead, we special case our FoiledModel, and just render everything else normally.
        var buffer = ItemRenderer.getFoilBuffer(buffers, Sheets.translucentCullBlockSheet(), true, model instanceof FoiledModel);

        for (var faceIdx = 0; faceIdx <= ModelHelper.NULL_FACE_ID; faceIdx++) {
            var face = ModelHelper.faceFromIndex(faceIdx);
            random.setSeed(42);
            ModelRenderer.renderQuads(transform, buffer, model.getQuads(null, face, random), lightmapCoord, overlayLight, tints);
        }
    }
}
