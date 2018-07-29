/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public abstract class TileMonitor extends TileGeneric implements ITickable, IPeripheralTile
{
    // Statics
    public static final double RENDER_BORDER = (2.0 / 16.0);
    public static final double RENDER_MARGIN = (0.5 / 16.0);
    public static final double RENDER_PIXEL_SCALE = (1.0 / 64.0);

    private static final int MAX_WIDTH = 8;
    private static final int MAX_HEIGHT = 6;

    // Members
    private ServerMonitor m_serverMonitor;
    private ClientMonitor m_clientMonitor;
    private MonitorPeripheral m_peripheral;
    private final Set<IComputerAccess> m_computers = new HashSet<>();

    private boolean m_destroyed = false;
    private boolean m_ignoreMe = false;

    private int m_width = 1;
    private int m_height = 1;
    private int m_xIndex = 0;
    private int m_yIndex = 0;

    private EnumFacing cachedFacing;
    private EnumFacing cachedOrientation;

    private final boolean advanced;

    public TileMonitor( boolean advanced )
    {
        this.advanced = advanced;
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        cachedFacing = null;
        cachedOrientation = null;
    }

    @Override
    public void destroy()
    {
        if( !m_destroyed )
        {
            m_destroyed = true;
            if( !getWorld().isRemote )
            {
                contractNeighbours();
            }
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        if( m_clientMonitor != null && m_xIndex == 0 && m_yIndex == 0 ) m_clientMonitor.destroy();
    }

    @Override
    public void onChunkUnload()
    {
        super.onChunkUnload();
        if( m_clientMonitor != null && m_xIndex == 0 && m_yIndex == 0 ) m_clientMonitor.destroy();
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( !player.isSneaking() && getFront() == side )
        {
            if( !getWorld().isRemote )
            {
                monitorTouched( hitX, hitY, hitZ );
            }
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        tag = super.writeToNBT( tag );
        tag.setInteger( "xIndex", m_xIndex );
        tag.setInteger( "yIndex", m_yIndex );
        tag.setInteger( "width", m_width );
        tag.setInteger( "height", m_height );
        return tag;
    }

    @Override
    public void readFromNBT( NBTTagCompound tag )
    {
        super.readFromNBT( tag );
        m_xIndex = tag.getInteger( "xIndex" );
        m_yIndex = tag.getInteger( "yIndex" );
        m_width = tag.getInteger( "width" );
        m_height = tag.getInteger( "height" );
    }

    @Override
    public void update()
    {
        if( !getWorld().isRemote )
        {
            if( m_xIndex == 0 && m_yIndex == 0 && m_serverMonitor != null )
            {
                if( m_serverMonitor.pollResized() )
                {
                    for( int x = 0; x < m_width; x++ )
                    {
                        for( int y = 0; y < m_height; y++ )
                        {
                            TileMonitor monitor = getNeighbour( x, y );
                            if( monitor == null ) continue;

                            for( IComputerAccess computer : monitor.m_computers )
                            {
                                computer.queueEvent( "monitor_resize", new Object[]{
                                    computer.getAttachmentName()
                                } );
                            }
                        }
                    }
                }

                m_serverMonitor.update();
                if( m_serverMonitor.hasTerminalChanged() ) updateBlock();
            }
        }
        else
        {
            if( m_xIndex == 0 && m_yIndex == 0 && m_clientMonitor != null )
            {
                m_clientMonitor.update();
            }
        }
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        createServerMonitor(); // Ensure the monitor is created before doing anything else.

        if( m_peripheral == null ) m_peripheral = new MonitorPeripheral( this );
        return m_peripheral;
    }

    public ServerMonitor getCachedServerMonitor()
    {
        return m_serverMonitor;
    }

    private ServerMonitor getServerMonitor()
    {
        if( m_serverMonitor != null ) return m_serverMonitor;

        TileMonitor origin = getOrigin();
        if( origin == null ) return null;

        return m_serverMonitor = origin.m_serverMonitor;
    }

    private ServerMonitor createServerMonitor()
    {
        if( m_serverMonitor != null )
        {
            return m_serverMonitor;
        }
        else if( m_xIndex == 0 && m_yIndex == 0 )
        {
            // If we're the origin, set up the new monitor
            m_serverMonitor = new ServerMonitor( advanced, this );
            m_serverMonitor.rebuild();

            // And propagate it to child monitors
            for( int x = 0; x < m_width; x++ )
            {
                for( int y = 0; y < m_height; y++ )
                {
                    TileMonitor monitor = getNeighbour( x, y );
                    if( monitor != null ) monitor.m_serverMonitor = m_serverMonitor;
                }
            }

            return m_serverMonitor;
        }
        else
        {
            // Otherwise fetch the origin and attempt to get its monitor
            // Note this may load chunks, but we don't really have a choice here.
            BlockPos pos = getPos();
            TileEntity te = world.getTileEntity( pos.offset( getRight(), -m_xIndex ).offset( getDown(), -m_yIndex ) );
            if( !(te instanceof TileMonitor) ) return null;

            return m_serverMonitor = ((TileMonitor) te).createServerMonitor();
        }
    }

    public ClientMonitor getClientMonitor()
    {
        if( m_clientMonitor != null ) return m_clientMonitor;

        BlockPos pos = getPos();
        TileEntity te = world.getTileEntity( pos.offset( getRight(), -m_xIndex ).offset( getDown(), -m_yIndex ) );
        if( !(te instanceof TileMonitor) ) return null;

        return m_clientMonitor = ((TileMonitor) te).m_clientMonitor;
    }

    // Networking stuff

    @Override
    public void writeDescription( @Nonnull NBTTagCompound tag )
    {
        super.writeDescription( tag );
        tag.setInteger( "xIndex", m_xIndex );
        tag.setInteger( "yIndex", m_yIndex );
        tag.setInteger( "width", m_width );
        tag.setInteger( "height", m_height );

        if( m_xIndex == 0 && m_yIndex == 0 && m_serverMonitor != null )
        {
            m_serverMonitor.writeDescription( tag );
        }
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );

        int oldXIndex = m_xIndex;
        int oldYIndex = m_yIndex;
        int oldWidth = m_width;
        int oldHeight = m_height;

        m_xIndex = tag.getInteger( "xIndex" );
        m_yIndex = tag.getInteger( "yIndex" );
        m_width = tag.getInteger( "width" );
        m_height = tag.getInteger( "height" );

        if( oldXIndex != m_xIndex || oldYIndex != m_yIndex )
        {
            // If our index has changed then it's possible the origin monitor has changed. Thus
            // we'll clear our cache. If we're the origin then we'll need to remove the glList as well.
            if( oldXIndex == 0 && oldYIndex == 0 && m_clientMonitor != null ) m_clientMonitor.destroy();
            m_clientMonitor = null;
        }

        if( m_xIndex == 0 && m_yIndex == 0 )
        {
            // If we're the origin terminal then read the description
            if( m_clientMonitor == null ) m_clientMonitor = new ClientMonitor( advanced, this );
            m_clientMonitor.readDescription( tag );
        }

        if( oldXIndex != m_xIndex || oldYIndex != m_yIndex ||
            oldWidth != m_width || oldHeight != m_height )
        {
            // One of our properties has changed, so ensure we redraw the block
            updateBlock();
        }
    }

    // Sizing and placement stuff

    private void fetchBlockInfo()
    {
        if( cachedOrientation == null )
        {
            IBlockState state = getBlockState();
            blockType = state.getBlock();
            cachedOrientation = state.getValue( BlockMonitor.ORIENTATION );
            cachedFacing = state.getValue( BlockMonitor.FACING );
        }
    }

    @Override
    public EnumFacing getDirection()
    {
        fetchBlockInfo();
        return cachedFacing;
    }

    private EnumFacing getOrientation()
    {
        fetchBlockInfo();
        return cachedOrientation;
    }

    public EnumFacing getFront()
    {
        EnumFacing orientation = getOrientation();
        return orientation == EnumFacing.NORTH ? getDirection() : orientation;
    }

    public EnumFacing getRight()
    {
        return getDirection().rotateYCCW();
    }

    private EnumFacing getDown()
    {
        EnumFacing orientation = getOrientation();
        switch( orientation )
        {
            default:
            case NORTH:
                return EnumFacing.UP;
            case UP:
                return getDirection().getOpposite();
            case DOWN:
                return getDirection();
        }
    }

    public int getWidth()
    {
        return m_width;
    }

    public int getHeight()
    {
        return m_height;
    }

    public int getXIndex()
    {
        return m_xIndex;
    }

    public int getYIndex()
    {
        return m_yIndex;
    }

    private TileMonitor getSimilarMonitorAt( BlockPos pos )
    {
        if( pos.equals( getPos() ) )
        {
            return this;
        }

        int y = pos.getY();
        World world = getWorld();
        if( world != null && y >= 0 && y < world.getHeight() )
        {
            if( world.isBlockLoaded( pos ) )
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileMonitor )
                {
                    TileMonitor monitor = (TileMonitor) tile;
                    if( monitor.getDirection() == getDirection() && monitor.getOrientation() == getOrientation() &&
                        monitor.getBlock() == getBlock() && !monitor.m_destroyed && !monitor.m_ignoreMe )
                    {
                        return monitor;
                    }
                }
            }

        }
        return null;
    }

    private TileMonitor getNeighbour( int x, int y )
    {
        BlockPos pos = getPos();
        EnumFacing right = getRight();
        EnumFacing down = getDown();
        int xOffset = -m_xIndex + x;
        int yOffset = -m_yIndex + y;
        return getSimilarMonitorAt(
            pos.offset( right, xOffset ).offset( down, yOffset )
        );
    }

    public TileMonitor getOrigin()
    {
        return getNeighbour( 0, 0 );
    }

    private void resize( int width, int height )
    {
        // If we're not already the origin then we'll need to generate a new terminal.
        if( m_xIndex != 0 || m_yIndex != 0 ) m_serverMonitor = null;

        m_xIndex = 0;
        m_yIndex = 0;
        m_width = width;
        m_height = height;

        // Determine if we actually need a monitor. In order to do this, simply check if
        // any component monitor been wrapped as a peripheral. Whilst this flag may be
        // out of date,
        boolean needsTerminal = false;
        terminalCheck:
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                TileMonitor monitor = getNeighbour( x, y );
                if( monitor != null && monitor.m_peripheral != null )
                {
                    needsTerminal = true;
                    break terminalCheck;
                }
            }
        }

        // Either delete the current monitor or sync a new one.
        if( needsTerminal )
        {
            if( m_serverMonitor == null ) m_serverMonitor = new ServerMonitor( advanced, this );
        }
        else
        {
            m_serverMonitor = null;
        }

        // Update the terminal's width and height and rebuild it. This ensures the monitor
        // is consistent when syncing it to other monitors.
        if( m_serverMonitor != null ) m_serverMonitor.rebuild();

        // Update the other monitors, setting coordinates, dimensions and the server terminal
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                TileMonitor monitor = getNeighbour( x, y );
                if( monitor == null ) continue;

                monitor.m_xIndex = x;
                monitor.m_yIndex = y;
                monitor.m_width = width;
                monitor.m_height = height;
                monitor.m_serverMonitor = m_serverMonitor;
                monitor.updateBlock();
            }
        }
    }

    private boolean mergeLeft()
    {
        TileMonitor left = getNeighbour( -1, 0 );
        if( left != null && left.m_yIndex == 0 && left.m_height == m_height )
        {
            int width = left.m_width + m_width;
            if( width <= MAX_WIDTH )
            {
                TileMonitor origin = left.getOrigin();
                if( origin != null ) origin.resize( width, m_height );
                left.expand();
                return true;
            }
        }
        return false;
    }

    private boolean mergeRight()
    {
        TileMonitor right = getNeighbour( m_width, 0 );
        if( right != null && right.m_yIndex == 0 && right.m_height == m_height )
        {
            int width = m_width + right.m_width;
            if( width <= MAX_WIDTH )
            {
                TileMonitor origin = getOrigin();
                if( origin != null ) origin.resize( width, m_height );
                expand();
                return true;
            }
        }
        return false;
    }

    private boolean mergeUp()
    {
        TileMonitor above = getNeighbour( 0, m_height );
        if( above != null && above.m_xIndex == 0 && above.m_width == m_width )
        {
            int height = above.m_height + m_height;
            if( height <= MAX_HEIGHT )
            {
                TileMonitor origin = getOrigin();
                if( origin != null ) origin.resize( m_width, height );
                expand();
                return true;
            }
        }
        return false;
    }

    private boolean mergeDown()
    {
        TileMonitor below = getNeighbour( 0, -1 );
        if( below != null && below.m_xIndex == 0 && below.m_width == m_width )
        {
            int height = m_height + below.m_height;
            if( height <= MAX_HEIGHT )
            {
                TileMonitor origin = below.getOrigin();
                if( origin != null ) origin.resize( m_width, height );
                below.expand();
                return true;
            }
        }
        return false;
    }

    public void expand()
    {
        while( mergeLeft() || mergeRight() || mergeUp() || mergeDown() )
        {
        }
    }

    public void contractNeighbours()
    {
        m_ignoreMe = true;
        if( m_xIndex > 0 )
        {
            TileMonitor left = getNeighbour( m_xIndex - 1, m_yIndex );
            if( left != null ) left.contract();
        }
        if( m_xIndex + 1 < m_width )
        {
            TileMonitor right = getNeighbour( m_xIndex + 1, m_yIndex );
            if( right != null ) right.contract();
        }
        if( m_yIndex > 0 )
        {
            TileMonitor below = getNeighbour( m_xIndex, m_yIndex - 1 );
            if( below != null ) below.contract();
        }
        if( m_yIndex + 1 < m_height )
        {
            TileMonitor above = getNeighbour( m_xIndex, m_yIndex + 1 );
            if( above != null ) above.contract();
        }
        m_ignoreMe = false;
    }

    public void contract()
    {
        int height = m_height;
        int width = m_width;

        TileMonitor origin = getOrigin();
        if( origin == null )
        {
            TileMonitor right = null;
            TileMonitor below = null;
            if( width > 1 ) right = getNeighbour( 1, 0 );
            if( height > 1 ) below = getNeighbour( 0, 1 );
            if( right != null ) right.resize( width - 1, 1 );
            if( below != null ) below.resize( width, height - 1 );
            if( right != null ) right.expand();
            if( below != null ) below.expand();
            return;
        }

        for( int y = 0; y < height; ++y )
        {
            for( int x = 0; x < width; ++x )
            {
                TileMonitor monitor = origin.getNeighbour( x, y );
                if( monitor == null )
                {
                    // Decompose
                    TileMonitor above = null;
                    TileMonitor left = null;
                    TileMonitor right = null;
                    TileMonitor below = null;

                    if( y > 0 )
                    {
                        above = origin;
                        above.resize( width, y );
                    }
                    if( x > 0 )
                    {
                        left = origin.getNeighbour( 0, y );
                        left.resize( x, 1 );
                    }
                    if( x + 1 < width )
                    {
                        right = origin.getNeighbour( x + 1, y );
                        right.resize( width - (x + 1), 1 );
                    }
                    if( y + 1 < height )
                    {
                        below = origin.getNeighbour( 0, y + 1 );
                        below.resize( width, height - (y + 1) );
                    }

                    // Re-expand
                    if( above != null ) above.expand();
                    if( left != null ) left.expand();
                    if( right != null ) right.expand();
                    if( below != null ) below.expand();
                    return;
                }
            }
        }
    }

    public void monitorTouched( float xPos, float yPos, float zPos )
    {
        XYPair pair = convertToXY( xPos, yPos, zPos, getDirection(), getOrientation() );
        pair = new XYPair( pair.x + m_xIndex, pair.y + m_height - m_yIndex - 1 );

        if( pair.x > (m_width - RENDER_BORDER) || pair.y > (m_height - RENDER_BORDER) || pair.x < (RENDER_BORDER) || pair.y < (RENDER_BORDER) )
        {
            return;
        }

        ServerTerminal serverTerminal = getServerMonitor();
        if( serverTerminal == null || !serverTerminal.isColour() ) return;

        Terminal originTerminal = serverTerminal.getTerminal();
        if( originTerminal == null ) return;

        double xCharWidth = (m_width - ((RENDER_BORDER + RENDER_MARGIN) * 2.0)) / originTerminal.getWidth();
        double yCharHeight = (m_height - ((RENDER_BORDER + RENDER_MARGIN) * 2.0)) / originTerminal.getHeight();

        int xCharPos = (int) Math.min( originTerminal.getWidth(), Math.max( ((pair.x - RENDER_BORDER - RENDER_MARGIN) / xCharWidth) + 1.0, 1.0 ) );
        int yCharPos = (int) Math.min( originTerminal.getHeight(), Math.max( ((pair.y - RENDER_BORDER - RENDER_MARGIN) / yCharHeight) + 1.0, 1.0 ) );

        for( int y = 0; y < m_height; ++y )
        {
            for( int x = 0; x < m_width; ++x )
            {
                TileMonitor monitor = getNeighbour( x, y );
                if( monitor == null ) continue;

                for( IComputerAccess computer : monitor.m_computers )
                {
                    computer.queueEvent( "monitor_touch", new Object[]{
                        computer.getAttachmentName(), xCharPos, yCharPos
                    } );
                }
            }
        }
    }

    private static XYPair convertToXY( float xPos, float yPos, float zPos, EnumFacing facing, EnumFacing orientation )
    {
        switch( orientation )
        {
            case NORTH:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, 1 - yPos );
                    case SOUTH:
                        return new XYPair( xPos, 1 - yPos );
                    case WEST:
                        return new XYPair( zPos, 1 - yPos );
                    case EAST:
                        return new XYPair( 1 - zPos, 1 - yPos );
                }
                break;
            case UP:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, zPos );
                    case SOUTH:
                        return new XYPair( xPos, 1 - zPos );
                    case WEST:
                        return new XYPair( zPos, xPos );
                    case EAST:
                        return new XYPair( 1 - zPos, 1 - xPos );
                }
                break;
            case DOWN:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, 1 - zPos );
                    case SOUTH:
                        return new XYPair( xPos, zPos );
                    case WEST:
                        return new XYPair( zPos, 1 - xPos );
                    case EAST:
                        return new XYPair( 1 - zPos, xPos );
                }
                break;
        }

        return new XYPair( xPos, yPos );
    }

    public void addComputer( IComputerAccess computer )
    {
        synchronized( this )
        {
            m_computers.add( computer );
        }
    }

    public void removeComputer( IComputerAccess computer )
    {
        synchronized( this )
        {
            m_computers.remove( computer );
        }
    }

    private static class XYPair
    {
        public final float x;
        public final float y;

        XYPair( float x, float y )
        {
            this.x = x;
            this.y = y;
        }
    }

    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        TileMonitor start = getNeighbour( 0, 0 );
        TileMonitor end = getNeighbour( m_width - 1, m_height - 1 );
        if( start != null && end != null )
        {
            BlockPos startPos = start.getPos();
            BlockPos endPos = end.getPos();
            int minX = Math.min( startPos.getX(), endPos.getX() );
            int minY = Math.min( startPos.getY(), endPos.getY() );
            int minZ = Math.min( startPos.getZ(), endPos.getZ() );
            int maxX = Math.max( startPos.getX(), endPos.getX() ) + 1;
            int maxY = Math.max( startPos.getY(), endPos.getY() ) + 1;
            int maxZ = Math.max( startPos.getZ(), endPos.getZ() ) + 1;
            return new AxisAlignedBB( minX, minY, minZ, maxX, maxY, maxZ );
        }
        else
        {
            BlockPos pos = this.getPos();
            return new AxisAlignedBB( pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1 );
        }
    }

    @Override
    public PeripheralType getPeripheralType()
    {
        return advanced ? PeripheralType.AdvancedMonitor : PeripheralType.Monitor;
    }

    @Override
    public String getLabel()
    {
        return null;
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
    }

    public static class TileMonitorNormal extends TileMonitor
    {
        public TileMonitorNormal()
        {
            super( false );
        }
    }

    public static class TileMonitorAdvanced extends TileMonitor
    {
        public TileMonitorAdvanced()
        {
            super( true );
        }
    }
}
