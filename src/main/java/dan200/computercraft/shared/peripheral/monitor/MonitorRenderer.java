/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import org.lwjgl.opengl.GL;

import javax.annotation.Nonnull;

/**
 * The render type to use for monitors.
 *
 * @see TileEntityMonitorRenderer
 * @see ClientMonitor
 */
public enum MonitorRenderer
{
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
    VBO;

    /**
     * Get the current renderer to use.
     *
     * @return The current renderer. Will not return {@link MonitorRenderer#BEST}.
     */
    @Nonnull
    public static MonitorRenderer current()
    {
        MonitorRenderer current = ComputerCraft.monitorRenderer;
        switch( current )
        {
            case BEST:
                return best();
            case TBO:
                checkCapabilities();
                if( !textureBuffer )
                {
                    ComputerCraft.log.warn( "Texture buffers are not supported on your graphics card. Falling back to default." );
                    ComputerCraft.monitorRenderer = BEST;
                    return best();
                }

                return TBO;
            default:
                return current;
        }
    }

    private static MonitorRenderer best()
    {
        checkCapabilities();
        return textureBuffer ? TBO : VBO;
    }

    private static boolean initialised = false;
    private static boolean textureBuffer = false;

    private static void checkCapabilities()
    {
        if( initialised ) return;

        textureBuffer = GL.getCapabilities().OpenGL31;
        initialised = true;
    }
}
