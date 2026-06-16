// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ClientRegistry;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Monitor support for {@link SubmitNodeCollection}.
 */
@Mixin(FeatureRenderDispatcher.class)
abstract class FeatureRenderDispatcherMixin {
    @Shadow
    @Final
    private FeatureRendererMap featureRenderers;

    @Inject(method = "<init>", at = @At("RETURN"))
    @SuppressWarnings("unused")
    private void registerExtendedFeatureRenderers(CallbackInfo ci) {
        ClientRegistry.registerFeatureRenderers(new ClientRegistry.RegisterFeatureRenderer() {
            @Override
            public <T extends SubmitNode> void register(FeatureRendererType<T> type, Supplier<FeatureRenderer<T>> renderer) {
                featureRenderers.put(type, renderer.get());
            }
        });
    }
}
