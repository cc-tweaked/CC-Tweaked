// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorRenderState;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.impl.client.ExtendedOrderedSubmitNodeCollector;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Monitor support for {@link SubmitNodeCollection}.
 */
@Mixin(SubmitNodeCollection.class)
abstract class SubmitNodeCollectionMixin implements ExtendedOrderedSubmitNodeCollector {
    @Shadow
    @Final
    public SimpleFeatureRenderPhase solid;

    @Override
    public void computercraft$submitMonitor(PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin) {
        var renderState = monitor.getRenderState(MonitorRenderState::new);
        solid.submit(new MonitorBlockEntityRenderer.MonitorSubmit(poseStack.last().copy(), monitor, terminal, renderState, xMargin, yMargin));
    }
}
