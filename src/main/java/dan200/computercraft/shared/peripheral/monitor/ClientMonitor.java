/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.client.util.DirectVertexBuffer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class ClientMonitor
{
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;

    public int tboBuffer;
    public int tboTexture;
    public int tboUniform;
    public DirectVertexBuffer backgroundBuffer;
    public DirectVertexBuffer foregroundBuffer;
    private NetworkedTerminal terminal;
    private boolean terminalChanged;

    public ClientMonitor( TileMonitor origin )
    {
        this.origin = origin;
    }

    public TileMonitor getOrigin()
    {
        return origin;
    }

    /**
     * Create the appropriate buffer if needed.
     *
     * @param renderer The renderer to use. This can be fetched from {@link MonitorRenderer#current()}.
     * @return If a buffer was created. This will return {@code false} if we already have an appropriate buffer,
     * or this mode does not require one.
     */
    @OnlyIn( Dist.CLIENT )
    public boolean createBuffer( MonitorRenderer renderer )
    {
        switch( renderer )
        {
            case TBO:
            {
                if( tboBuffer != 0 ) return false;

                deleteBuffers();

                tboBuffer = DirectBuffers.createBuffer();
                DirectBuffers.setEmptyBufferData( GL31.GL_TEXTURE_BUFFER, tboBuffer, GL15.GL_STATIC_DRAW );
                tboTexture = GlStateManager._genTexture();
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, tboTexture );
                GL31.glTexBuffer( GL31.GL_TEXTURE_BUFFER, GL30.GL_R8UI, tboBuffer );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, 0 );

                tboUniform = DirectBuffers.createBuffer();
                DirectBuffers.setEmptyBufferData( GL31.GL_UNIFORM_BUFFER, tboUniform, GL15.GL_STATIC_DRAW );

                addMonitor();
                return true;
            }

            case VBO:
                if( backgroundBuffer != null ) return false;

                deleteBuffers();
                backgroundBuffer = new DirectVertexBuffer();
                foregroundBuffer = new DirectVertexBuffer();
                addMonitor();
                return true;

            default:
                return false;
        }
    }

    private void addMonitor()
    {
        synchronized( allMonitors )
        {
            allMonitors.add( this );
        }
    }

    private void deleteBuffers()
    {

        if( tboBuffer != 0 )
        {
            DirectBuffers.deleteBuffer( GL31.GL_TEXTURE_BUFFER, tboBuffer );
            tboBuffer = 0;
        }

        if( tboTexture != 0 )
        {
            GlStateManager._deleteTexture( tboTexture );
            tboTexture = 0;
        }

        if( tboUniform != 0 )
        {
            DirectBuffers.deleteBuffer( GL31.GL_UNIFORM_BUFFER, tboUniform );
            tboUniform = 0;
        }

        if( backgroundBuffer != null )
        {
            backgroundBuffer.close();
            backgroundBuffer = null;
        }

        if( foregroundBuffer != null )
        {
            foregroundBuffer.close();
            foregroundBuffer = null;
        }
    }

    @OnlyIn( Dist.CLIENT )
    public void destroy()
    {
        if( tboBuffer != 0 || backgroundBuffer != null )
        {
            synchronized( allMonitors )
            {
                allMonitors.remove( this );
            }

            deleteBuffers();
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
                monitor.deleteBuffers();

                iterator.remove();
            }
        }
    }

    public boolean pollTerminalChanged()
    {
        boolean changed = terminalChanged;
        terminalChanged = false;
        return changed;
    }

    public Terminal getTerminal()
    {
        return terminal;
    }

    void read( TerminalState state )
    {
        if( state.hasTerminal() )
        {
            if( terminal == null ) terminal = new NetworkedTerminal( state.width, state.height, state.colour );
            state.apply( terminal );
            terminalChanged = true;
        }
        else
        {
            if( terminal != null )
            {
                terminal = null;
                terminalChanged = true;
            }
        }
    }
}
