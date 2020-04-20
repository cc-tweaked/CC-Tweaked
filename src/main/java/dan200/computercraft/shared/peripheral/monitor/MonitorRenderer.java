/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import javax.annotation.Nonnull;
import java.util.Locale;

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
     * Render using VBOs. This is the default when supported.
     *
     * @see net.minecraft.client.renderer.vertex.VertexBuffer
     */
    VBO,

    /**
     * Render using display lists.
     *
     * @see net.minecraft.client.renderer.GLAllocation#generateDisplayLists(int)
     */
    DISPLAY_LIST,

    /**
     * Draw directly using the tessellator. This will never be selected by default, but is useful as a way to check
     * performance.
     *
     * @see Tessellator#draw()
     */
    DIRECT;

    private static final MonitorRenderer[] VALUES = values();
    public static final String[] NAMES;

    private final String displayName = "gui.computercraft:config.peripheral.monitor_renderer." + name().toLowerCase( Locale.ROOT );

    static
    {
        NAMES = new String[VALUES.length];
        for( int i = 0; i < VALUES.length; i++ ) NAMES[i] = VALUES[i].displayName();
    }

    public String displayName()
    {
        return displayName;
    }

    @Nonnull
    public static MonitorRenderer ofString( String name )
    {
        for( MonitorRenderer backend : VALUES )
        {
            if( backend.displayName.equalsIgnoreCase( name ) || backend.name().equalsIgnoreCase( name ) )
            {
                return backend;
            }
        }

        ComputerCraft.log.warn( "Unknown monitor renderer {}. Falling back to default.", name );
        return BEST;
    }

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
            case VBO:
                if( !OpenGlHelper.vboSupported )
                {
                    ComputerCraft.log.warn( "VBOs are not supported on your graphics card. Falling back to default." );
                    ComputerCraft.monitorRenderer = BEST;
                    return best();
                }

                return VBO;
            default:
                return current;
        }
    }

    private static MonitorRenderer best()
    {
        return OpenGlHelper.vboSupported ? VBO : DISPLAY_LIST;
    }
}
