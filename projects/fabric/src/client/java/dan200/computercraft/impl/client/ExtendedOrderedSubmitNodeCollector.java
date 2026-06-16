// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;

/**
 * Extension interface to {@link OrderedSubmitNodeCollector} that allows submitting monitor contents.
 *
 * @see MonitorBlockEntityRenderer
 */
public interface ExtendedOrderedSubmitNodeCollector {
    default void computercraft$submitMonitor(
        PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin
    ) {
        var collector = (OrderedSubmitNodeCollector) this;
        collector.submitCustomGeometry(poseStack, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) -> {
            FixedWidthFontRenderer.drawTerminalBackground(pose.pose(), buffer, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin);
        });
        collector.submitCustomGeometry(poseStack, FixedWidthFontRenderer.TERMINAL_TEXT_OFFSET, (pose, buffer) -> {
            FixedWidthFontRenderer.drawTerminalForeground(pose.pose(), buffer, 0, 0, terminal);
            FixedWidthFontRenderer.drawCursor(pose.pose(), buffer, 0, 0, terminal);
        });
    }
}
