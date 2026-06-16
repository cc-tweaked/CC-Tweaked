// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorRenderState;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.resources.model.ModelDebugName;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

@AutoService(ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    @Override
    public <T> ModelKey<T> createModelKey(ModelDebugName name) {
        return new ForgeModelKey<>(new StandaloneModelKey<T>(name));
    }

    @Override
    public void submitMonitor(OrderedSubmitNodeCollector collector, PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin) {
        var renderState = monitor.getRenderState(MonitorRenderState::new);
        collector.submitSpecial(
            RenderPhaseKeys.SOLID, new MonitorBlockEntityRenderer.MonitorSubmit(poseStack.last().copy(), monitor, terminal, renderState, xMargin, yMargin)
        );
    }
}
