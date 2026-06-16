// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.impl.client.ExtendedOrderedSubmitNodeCollector;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.resources.model.ModelDebugName;

@AutoService(ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    @Override
    public <T> ModelKey<T> createModelKey(ModelDebugName name) {
        return new FabricModelKey<>(ExtraModelKey.create(name::debugName));
    }

    @Override
    public void submitMonitor(OrderedSubmitNodeCollector collector, PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin) {
        ((ExtendedOrderedSubmitNodeCollector) collector).computercraft$submitMonitor(poseStack, monitor, terminal, xMargin, yMargin);
    }
}
