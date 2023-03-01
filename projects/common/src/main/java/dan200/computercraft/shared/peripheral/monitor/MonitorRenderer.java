// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.monitor;

/**
 * The render type to use for monitors.
 *
 * @see dan200.computercraft.client.render.TileEntityMonitorRenderer
 * @see ClientMonitor
 */
public enum MonitorRenderer {
    /**
     * Determine the best monitor backend.
     */
    BEST,

    /**
     * Render using texture buffer objects.
     *
     * @see org.lwjgl.opengl.GL31#glTexBuffer(int, int, int)
     */
    TBO,

    /**
     * Render using VBOs.
     *
     * @see com.mojang.blaze3d.vertex.VertexBuffer
     */
    VBO,
}
