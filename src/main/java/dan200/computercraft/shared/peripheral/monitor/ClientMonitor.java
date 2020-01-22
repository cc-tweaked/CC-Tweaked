/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.common.ClientTerminal;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientMonitor extends ClientTerminal
{
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;
    public VertexBuffer buffer = null;

    public ClientMonitor( boolean colour, TileMonitor origin )
    {
        super( colour );
        this.origin = origin;
    }

    public TileMonitor getOrigin()
    {
        return origin;
    }

    @OnlyIn( Dist.CLIENT )
    public void createBuffer()
    {
        if( buffer == null )
        {
            buffer = new VertexBuffer( FixedWidthFontRenderer.TYPE.getVertexFormat() );
            synchronized( allMonitors )
            {
                allMonitors.add( this );
            }
        }
    }

    @OnlyIn( Dist.CLIENT )
    public void destroy()
    {
        if( buffer != null )
        {
            synchronized( allMonitors )
            {
                allMonitors.remove( this );
            }

            buffer.close();
            buffer = null;
        }
    }

    @OnlyIn( Dist.CLIENT )
    public static void destroyAll()
    {
        synchronized( allMonitors )
        {
            for( Iterator<ClientMonitor> iterator = allMonitors.iterator(); iterator.hasNext(); )
            {
                ClientMonitor monitor = iterator.next();
                if( monitor.buffer != null )
                {
                    monitor.buffer.close();
                    monitor.buffer = null;
                }

                iterator.remove();
            }
        }
    }
}
