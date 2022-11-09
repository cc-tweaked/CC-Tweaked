/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
