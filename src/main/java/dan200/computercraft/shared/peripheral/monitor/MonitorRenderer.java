/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import net.fabricmc.loader.api.FabricLoader;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

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
        if( !initialised ) initialise();

        MonitorRenderer current = ComputerCraft.monitorRenderer;
        if( current == BEST ) return best();
        return current;
    }

    private static MonitorRenderer best()
    {
        if( shaderMod )
        {
            ComputerCraft.log.warn( "Shader mod detected. Enabling VBO monitor renderer for compatibility." );
            return ComputerCraft.monitorRenderer = VBO;
        }
        return ComputerCraft.monitorRenderer = TBO;
    }

    private static boolean initialised = false;
    private static boolean shaderMod;
    private static final List<String> shaderModIds = Arrays.asList( "iris", "canvas", "optifabric" );

    private static void initialise()
    {
        shaderMod = FabricLoader.getInstance().getAllMods().stream()
            .map( modContainer -> modContainer.getMetadata().getId() )
            .anyMatch( shaderModIds::contains );

        initialised = true;
    }
}
