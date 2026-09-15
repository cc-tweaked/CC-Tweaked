// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.ClientHooks;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
class FirstPersonHandsAndItemsRendererMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unused")
    private void onSubmitArmWithItem(
        PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state,
        float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
        float equippedProgress, PoseStack transform, SubmitNodeCollector collector, int combinedLight, CallbackInfo ci
    ) {
        if (ClientHooks.onRenderHeldItem(
            (FirstPersonHandsAndItemsRenderer) (Object) this, playerState, transform, collector,
            combinedLight, hand, pitch, equippedProgress, swingProgress, stack
        )) {
            ci.cancel();
        }
    }
}
