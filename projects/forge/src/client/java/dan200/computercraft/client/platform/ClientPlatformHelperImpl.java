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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.Arrays;

@AutoService(dan200.computercraft.impl.client.ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    private static final RandomSource random = RandomSource.create(0);
    private static final Direction[] directions = Arrays.copyOf(Direction.values(), 7);

    @Override
    public BakedModel getModel(ModelManager manager, ResourceLocation location) {
        return manager.getModel(location);
    }

    @Override
    public BakedModel createdFoiledModel(BakedModel model) {
        return new FoiledModel(model);
    }

    @Override
    public Packet<ServerGamePacketListener> createPacket(NetworkMessage<ServerNetworkContext> message) {
        return NetworkHandler.createServerboundPacket(message);
    }

    @Override
    public void renderBakedModel(PoseStack transform, MultiBufferSource buffers, BakedModel model, int lightmapCoord, int overlayLight, @Nullable int[] tints) {
        for (var renderType : model.getRenderTypes(ItemStack.EMPTY, true)) {
            var buffer = buffers.getBuffer(renderType);
            for (var face : directions) {
                random.setSeed(42);
                var quads = model.getQuads(null, face, random, ModelData.EMPTY, renderType);
                ModelRenderer.renderQuads(transform, buffer, quads, lightmapCoord, overlayLight, tints);
            }
        }
    }

    @Override
    public void playStreamingMusic(BlockPos pos, @Nullable SoundEvent sound) {
        Minecraft.getInstance().levelRenderer.playStreamingMusic(sound, pos, null);
    }
}
