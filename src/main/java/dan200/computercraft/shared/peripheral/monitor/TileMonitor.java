/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class TileMonitor extends TilePeripheralBase
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
    private boolean hasPeripheral;
    private final Set<IComputerAccess> m_computers;

    private boolean m_destroyed;
    private boolean m_ignoreMe;

    private int m_width;
    private int m_height;
    private int m_xIndex;
    private int m_yIndex;

    private int m_dir;

    private boolean m_advanced;

    public TileMonitor()
    {
        m_computers = new HashSet<>();

        m_destroyed = false;
        m_ignoreMe = false;
        
        m_width = 1;
        m_height = 1;
        m_xIndex = 0;
        m_yIndex = 0;
        
        m_dir = 2;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        m_advanced = getBlockState().getValue( BlockPeripheral.Properties.VARIANT )
            .getPeripheralType() == PeripheralType.AdvancedMonitor;
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
    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        nbttagcompound = super.writeToNBT( nbttagcompound);
        nbttagcompound.setInteger( "xIndex", m_xIndex );
        nbttagcompound.setInteger( "yIndex", m_yIndex );
        nbttagcompound.setInteger( "width", m_width );
        nbttagcompound.setInteger( "height", m_height );
        nbttagcompound.setInteger( "dir", m_dir );
        return nbttagcompound;
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        super.readFromNBT(nbttagcompound);
        m_xIndex = nbttagcompound.getInteger("xIndex");
        m_yIndex = nbttagcompound.getInteger("yIndex");
        m_width = nbttagcompound.getInteger("width");
        m_height = nbttagcompound.getInteger("height");
        m_dir = nbttagcompound.getInteger("dir");
    }

    @Override
    public void update()
    {
        super.update();

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
                                computer.queueEvent( "monitor_resize", new Object[] {
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
        hasPeripheral = true;
        createServerMonitor(); // Ensure the monitor is created before doing anything else.
        return new MonitorPeripheral( this );
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
            m_serverMonitor = new ServerMonitor( m_advanced, this );
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
    public void writeDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.writeDescription( nbttagcompound );
        nbttagcompound.setInteger( "xIndex", m_xIndex );
        nbttagcompound.setInteger( "yIndex", m_yIndex );
        nbttagcompound.setInteger( "width", m_width );
        nbttagcompound.setInteger( "height", m_height );
        nbttagcompound.setInteger( "monitorDir", m_dir );

        if( m_xIndex == 0 && m_yIndex == 0 && m_serverMonitor != null )
        {
            m_serverMonitor.writeDescription( nbttagcompound );
        }
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );

        int oldXIndex = m_xIndex;
        int oldYIndex = m_yIndex;
        int oldWidth = m_width;
        int oldHeight = m_height;
        int oldDir = m_dir;

        m_xIndex = nbttagcompound.getInteger( "xIndex" );
        m_yIndex = nbttagcompound.getInteger( "yIndex" );
        m_width = nbttagcompound.getInteger( "width" );
        m_height = nbttagcompound.getInteger( "height" );
        m_dir = nbttagcompound.getInteger( "monitorDir" );

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
            if( m_clientMonitor == null ) m_clientMonitor = new ClientMonitor( m_advanced, this );
            m_clientMonitor.readDescription( nbttagcompound );
        }

        if( oldXIndex != m_xIndex || oldYIndex != m_yIndex ||
            oldWidth != m_width || oldHeight != m_height ||
            oldDir != m_dir )
        {
            // One of our properties has changed, so ensure we redraw the block
            updateBlock();
        }
    }

    // Sizing and placement stuff

    @Override
    public EnumFacing getDirection()
    {
        int dir = getDir() % 6;
        switch( dir ) {
            case 2: return EnumFacing.NORTH;
            case 3: return EnumFacing.SOUTH;
            case 4: return EnumFacing.WEST;
            case 5: return EnumFacing.EAST;
        }
        return EnumFacing.NORTH;
    }

    public int getDir()
    {
        return m_dir;
    }
    
    public void setDir( int dir )
    {
        m_dir = dir;
        markDirty();
    }

    public EnumFacing getFront()
    {
        return m_dir <= 5 ? EnumFacing.getFront( m_dir ) : (m_dir <= 11 ? EnumFacing.DOWN : EnumFacing.UP);
    }

    public EnumFacing getRight()
    {
        int dir = getDir() % 6;
        switch( dir ) {
            case 2: return EnumFacing.WEST;
            case 3: return EnumFacing.EAST;
            case 4: return EnumFacing.SOUTH;
            case 5: return EnumFacing.NORTH;
        }
        return EnumFacing.WEST;
    }
    
    private EnumFacing getDown()
    {
        int dir = getDir();
        if (dir <= 5) return EnumFacing.UP;
        
        switch( dir ) {
            // up facing
            case 8: return EnumFacing.NORTH;
            case 9: return EnumFacing.SOUTH;
            case 10: return EnumFacing.WEST;
            case 11: return EnumFacing.EAST;
            // down facing
            case 14: return EnumFacing.SOUTH;
            case 15: return EnumFacing.NORTH;
            case 16: return EnumFacing.EAST;
            case 17: return EnumFacing.WEST;
        }
        return EnumFacing.NORTH;
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
                if( tile != null && tile instanceof TileMonitor )
                {
                    TileMonitor monitor = (TileMonitor) tile;
                    if( monitor.getDir() == getDir() && monitor.m_advanced == m_advanced &&
                        !monitor.m_destroyed && !monitor.m_ignoreMe )
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
                if( monitor != null && monitor.hasPeripheral )
                {
                    needsTerminal = true;
                    break terminalCheck;
                }
            }
        }

        // Either delete the current monitor or sync a new one.
        if( needsTerminal )
        {
            if( m_serverMonitor == null ) m_serverMonitor = new ServerMonitor( m_advanced, this );
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
        TileMonitor left = getNeighbour( -1,0 );
        if( left != null && left.m_yIndex == 0 && left.m_height == m_height )
        {
            int width = left.m_width + m_width;
            if( width <= MAX_WIDTH )
            {
                TileMonitor origin = left.getOrigin();
                if( origin != null )
                {
                    origin.resize( width, m_height );
                }
                left.expand();
                return true;
            }
        }
        return false;
    }
    
    private boolean mergeRight()
    {
        TileMonitor right = getNeighbour( m_width,0 );
        if( right != null && right.m_yIndex == 0 && right.m_height == m_height )
        {
            int width = m_width + right.m_width;
            if( width <= MAX_WIDTH )
            {
                TileMonitor origin = getOrigin();
                if( origin != null )
                {
                    origin.resize( width, m_height );
                }
                expand();
                return true;
            }
        }
        return false;
    }
    
    private boolean mergeUp()
    {
        TileMonitor above = getNeighbour( 0,m_height );
        if( above != null && above.m_xIndex == 0 && above.m_width == m_width )
        {
            int height = above.m_height + m_height;
            if( height <= MAX_HEIGHT)
            {
                TileMonitor origin = getOrigin();
                if( origin != null )
                {
                    origin.resize( m_width, height );
                }
                expand();
                return true;
            }
        }
        return false;
    }
    
    private boolean mergeDown()
    {
        TileMonitor below = getNeighbour( 0,-1 );
        if( below != null && below.m_xIndex == 0 && below.m_width == m_width )
        {
            int height = m_height + below.m_height;
            if( height <= MAX_HEIGHT )
            {
                TileMonitor origin = below.getOrigin();
                if( origin != null )
                {
                    origin.resize( m_width, height );
                }
                below.expand();
                return true;
            }
        }
        return false;
    }
    
    public void expand()
    {
        while( mergeLeft() || mergeRight() || mergeUp() || mergeDown() ) {}
    }
    
    public void contractNeighbours()
    {
        m_ignoreMe = true;
        if( m_xIndex > 0 ) {
            TileMonitor left = getNeighbour( m_xIndex - 1, m_yIndex );
            if( left != null ) {
                left.contract( );
            }
        }
        if( m_xIndex + 1 < m_width ) {
            TileMonitor right = getNeighbour( m_xIndex + 1, m_yIndex );
            if( right != null ) {
                right.contract();
            }
        }
        if( m_yIndex > 0 ) {
            TileMonitor below = getNeighbour( m_xIndex, m_yIndex - 1 );
            if( below != null ) {
                below.contract();
            }
        }
        if( m_yIndex + 1 < m_height ) {
            TileMonitor above = getNeighbour( m_xIndex, m_yIndex + 1 );
            if( above != null ) {
                above.contract();
            }
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
            if( width > 1 ) {
                right = getNeighbour( 1, 0 );
            }
            if( height > 1 ) {
                below = getNeighbour( 0, 1 );
            }
            if( right != null ) {
                right.resize( width - 1, 1 );
            }
            if( below != null ) {
                below.resize( width, height - 1 );
            }
            if( right != null ) {
                right.expand();
            }
            if( below != null ) {
                below.expand();
            }
            return;
        }
        
        for( int y=0; y<height; ++y )
        {
            for( int x=0; x<width; ++x )
            {
                TileMonitor monitor = origin.getNeighbour( x, y );
                if( monitor == null )
                {
                    // Decompose
                    TileMonitor above = null;
                    TileMonitor left = null;
                    TileMonitor right = null;
                    TileMonitor below = null;
                    
                       if( y > 0 ) {
                        above = origin;
                        above.resize( width, y );
                    }
                    if( x > 0 ) {
                        left = origin.getNeighbour( 0, y );
                        left.resize( x, 1 );
                    }
                    if( x + 1 < width ) {
                        right = origin.getNeighbour( x + 1, y );
                        right.resize( width - (x + 1), 1 );
                    }
                    if( y + 1 < height ) {
                        below = origin.getNeighbour( 0, y + 1 );
                        below.resize( width, height - (y + 1) );
                    }

                    // Re-expand
                    if( above != null ) {
                        above.expand();
                    }
                    if( left != null ) {
                        left.expand();
                    }
                    if( right != null ) {
                        right.expand();
                    }
                    if( below != null ) {
                        below.expand();
                    }
                    return;
                }                
            }
        }
    }
    
    public void monitorTouched( float xPos, float yPos, float zPos )
    {
        int side = getDir();
        XYPair pair = convertToXY( xPos, yPos, zPos, side );
        pair = new XYPair( pair.x + m_xIndex, pair.y + m_height - m_yIndex - 1 );

        if (pair.x > (m_width - RENDER_BORDER) || pair.y > (m_height - RENDER_BORDER) || pair.x < (RENDER_BORDER) || pair.y < (RENDER_BORDER))
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
                if( monitor == null )continue;

                for( IComputerAccess computer : monitor.m_computers )
                {
                    computer.queueEvent( "monitor_touch", new Object[] {
                        computer.getAttachmentName(), xCharPos, yCharPos
                    } );
                }
            }
        }
    }

    private XYPair convertToXY( float xPos, float yPos, float zPos, int side )
    {
        switch (side)
        {
        case 2:
            return new XYPair( 1 - xPos, 1 - yPos );
        case 3:
            return new XYPair( xPos, 1 - yPos );
        case 4:
            return new XYPair( zPos, 1 - yPos );
        case 5:
            return new XYPair( 1 - zPos, 1 - yPos );
        case 8:
            return new XYPair( 1 - xPos, zPos );
        case 9:
            return new XYPair( xPos, 1 - zPos );
        case 10:
            return new XYPair( zPos, xPos );
        case 11:
            return new XYPair( 1 - zPos, 1 - xPos );
        case 14:
            return new XYPair( 1 - xPos, 1 - zPos );
        case 15:
            return new XYPair( xPos, zPos );
        case 16:
            return new XYPair( zPos, 1 - xPos );
        case 17:
            return new XYPair( 1 - zPos, xPos );
        default:
            return new XYPair( xPos, zPos );
        }
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
    
    public static class XYPair
    {
        public final float x;
        public final float y;

        private XYPair( float x, float y )
        {
            this.x = x;
            this.y = y;
        }
    }
    
    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        TileMonitor start = getNeighbour(0, 0);
        TileMonitor end = getNeighbour(m_width - 1, m_height - 1);
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
    public boolean shouldRefresh( World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState )
    {
        if( super.shouldRefresh( world, pos, oldState, newState ) )
        {
            return true;
        }
        else
        {
            switch( ComputerCraft.Blocks.peripheral.getPeripheralType( newState ) )
            {
                case Monitor:
                case AdvancedMonitor:
                {
                    return false;
                }
                default:
                {
                    return true;
                }
            }
        }
    }

}
