// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.impl.client.ExtendedOrderedSubmitNodeCollector;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Monitor support for {@link SubmitNodeCollector}.
 */
@Mixin(SubmitNodeCollector.class)
interface SubmitNodeCollectorMixin extends ExtendedOrderedSubmitNodeCollector {
    @Override
    default void computercraft$submitMonitor(PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin) {
        ((ExtendedOrderedSubmitNodeCollector) ((SubmitNodeCollector) this).order(0)).computercraft$submitMonitor(poseStack, monitor, terminal, xMargin, yMargin);
    }
}
