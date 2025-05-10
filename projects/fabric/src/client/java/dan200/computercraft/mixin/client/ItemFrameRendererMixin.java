// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.ClientHooks;
import dan200.computercraft.client.ExtendedItemFrameRenderStateHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
@SuppressWarnings("UnusedMethod")
class ItemFrameRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;mapId:Lnet/minecraft/world/level/saveddata/maps/MapId;", opcode = Opcodes.GETFIELD, ordinal = 1),
        cancellable = true
    )
    @SuppressWarnings("unused")
    private void render(ItemFrameRenderState frame, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        var state = ((ExtendedItemFrameRenderStateHolder) frame).computercraft$state();
        if (ClientHooks.onRenderItemFrame(pose, buffers, frame, state, light)) {
            ci.cancel();
            pose.popPose();
        }
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
        at = @At("HEAD")
    )
    @SuppressWarnings("unused")
    private void extractRenderState(ItemFrame entity, ItemFrameRenderState state, float f, CallbackInfo ci) {
        ((ExtendedItemFrameRenderStateHolder) state).computercraft$state().setup(entity.getItem());
    }
}
