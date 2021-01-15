/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.client.TerminalState;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileMonitor extends TileGeneric
{
    public static final double RENDER_BORDER = 2.0 / 16.0;
    public static final double RENDER_MARGIN = 0.5 / 16.0;
    public static final double RENDER_PIXEL_SCALE = 1.0 / 64.0;

    private static final String NBT_X = "XIndex";
    private static final String NBT_Y = "YIndex";
    private static final String NBT_WIDTH = "Width";
    private static final String NBT_HEIGHT = "Height";

    private final boolean advanced;

    private ServerMonitor serverMonitor;
    private ClientMonitor clientMonitor;
    private MonitorPeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;
    private final Set<IComputerAccess> computers = new HashSet<>();

    private boolean destroyed = false;
    private boolean visiting = false;

    // MonitorWatcher state.
    boolean enqueued;
    TerminalState cached;

    private int width = 1;
    private int height = 1;
    private int xIndex = 0;
    private int yIndex = 0;

    public TileMonitor( TileEntityType<? extends TileMonitor> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        TickScheduler.schedule( this );
    }

    @Override
    public void destroy()
    {
        // TODO: Call this before using the block
        if( destroyed ) return;
        destroyed = true;
        if( !getLevel().isClientSide ) contractNeighbours();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        if( clientMonitor != null && xIndex == 0 && yIndex == 0 ) clientMonitor.destroy();
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        if( clientMonitor != null && xIndex == 0 && yIndex == 0 ) clientMonitor.destroy();
    }

    @Nonnull
    @Override
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        if( !player.isCrouching() && getFront() == hit.getDirection() )
        {
            if( !getLevel().isClientSide )
            {
                monitorTouched(
                    (float) (hit.getLocation().x - hit.getBlockPos().getX()),
                    (float) (hit.getLocation().y - hit.getBlockPos().getY()),
                    (float) (hit.getLocation().z - hit.getBlockPos().getZ())
                );
            }
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.PASS;
    }

    @Nonnull
    @Override
    public CompoundNBT save( CompoundNBT tag )
    {
        tag.putInt( NBT_X, xIndex );
        tag.putInt( NBT_Y, yIndex );
        tag.putInt( NBT_WIDTH, width );
        tag.putInt( NBT_HEIGHT, height );
        return super.save( tag );
    }

    @Override
    public void load( @Nonnull CompoundNBT tag )
    {
        super.load( tag );
        xIndex = tag.getInt( NBT_X );
        yIndex = tag.getInt( NBT_Y );
        width = tag.getInt( NBT_WIDTH );
        height = tag.getInt( NBT_HEIGHT );
    }

    @Override
    public void blockTick()
    {
        if( xIndex != 0 || yIndex != 0 || serverMonitor == null ) return;

        serverMonitor.clearChanged();

        if( serverMonitor.pollResized() )
        {
            for( int x = 0; x < width; x++ )
            {
                for( int y = 0; y < height; y++ )
                {
                    TileMonitor monitor = getNeighbour( x, y );
                    if( monitor == null ) continue;

                    for( IComputerAccess computer : monitor.computers )
                    {
                        computer.queueEvent( "monitor_resize", computer.getAttachmentName() );
                    }
                }
            }
        }

        if( serverMonitor.pollTerminalChanged() ) MonitorWatcher.enqueue( this );
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        peripheralCap = CapabilityUtil.invalidate( peripheralCap );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            createServerMonitor(); // Ensure the monitor is created before doing anything else.
            if( peripheral == null ) peripheral = new MonitorPeripheral( this );
            if( peripheralCap == null ) peripheralCap = LazyOptional.of( () -> peripheral );
            return peripheralCap.cast();
        }
        return super.getCapability( cap, side );
    }

    public ServerMonitor getCachedServerMonitor()
    {
        return serverMonitor;
    }

    private ServerMonitor getServerMonitor()
    {
        if( serverMonitor != null ) return serverMonitor;

        TileMonitor origin = getOrigin();
        if( origin == null ) return null;

        return serverMonitor = origin.serverMonitor;
    }

    private ServerMonitor createServerMonitor()
    {
        if( serverMonitor != null ) return serverMonitor;

        if( xIndex == 0 && yIndex == 0 )
        {
            // If we're the origin, set up the new monitor
            serverMonitor = new ServerMonitor( advanced, this );
            serverMonitor.rebuild();

            // And propagate it to child monitors
            for( int x = 0; x < width; x++ )
            {
                for( int y = 0; y < height; y++ )
                {
                    TileMonitor monitor = getNeighbour( x, y );
                    if( monitor != null ) monitor.serverMonitor = serverMonitor;
                }
            }

            return serverMonitor;
        }
        else
        {
            // Otherwise fetch the origin and attempt to get its monitor
            // Note this may load chunks, but we don't really have a choice here.
            BlockPos pos = getBlockPos();
            TileEntity te = level.getBlockEntity( pos.relative( getRight(), -xIndex ).relative( getDown(), -yIndex ) );
            if( !(te instanceof TileMonitor) ) return null;

            return serverMonitor = ((TileMonitor) te).createServerMonitor();
        }
    }

    public ClientMonitor getClientMonitor()
    {
        if( clientMonitor != null ) return clientMonitor;

        BlockPos pos = getBlockPos();
        TileEntity te = level.getBlockEntity( pos.relative( getRight(), -xIndex ).relative( getDown(), -yIndex ) );
        if( !(te instanceof TileMonitor) ) return null;

        return clientMonitor = ((TileMonitor) te).clientMonitor;
    }

    // Networking stuff

    @Override
    protected void writeDescription( @Nonnull CompoundNBT nbt )
    {
        super.writeDescription( nbt );
        nbt.putInt( NBT_X, xIndex );
        nbt.putInt( NBT_Y, yIndex );
        nbt.putInt( NBT_WIDTH, width );
        nbt.putInt( NBT_HEIGHT, height );
    }

    @Override
    protected final void readDescription( @Nonnull CompoundNBT nbt )
    {
        super.readDescription( nbt );

        int oldXIndex = xIndex;
        int oldYIndex = yIndex;
        int oldWidth = width;
        int oldHeight = height;

        xIndex = nbt.getInt( NBT_X );
        yIndex = nbt.getInt( NBT_Y );
        width = nbt.getInt( NBT_WIDTH );
        height = nbt.getInt( NBT_HEIGHT );

        if( oldXIndex != xIndex || oldYIndex != yIndex )
        {
            // If our index has changed then it's possible the origin monitor has changed. Thus
            // we'll clear our cache. If we're the origin then we'll need to remove the glList as well.
            if( oldXIndex == 0 && oldYIndex == 0 && clientMonitor != null ) clientMonitor.destroy();
            clientMonitor = null;
        }

        if( xIndex == 0 && yIndex == 0 )
        {
            // If we're the origin terminal then create it.
            if( clientMonitor == null ) clientMonitor = new ClientMonitor( advanced, this );
        }

        if( oldXIndex != xIndex || oldYIndex != yIndex ||
            oldWidth != width || oldHeight != height )
        {
            // One of our properties has changed, so ensure we redraw the block
            updateBlock();
        }
    }

    public final void read( TerminalState state )
    {
        if( xIndex != 0 || yIndex != 0 )
        {
            ComputerCraft.log.warn( "Receiving monitor state for non-origin terminal at {}", getBlockPos() );
            return;
        }

        if( clientMonitor == null ) clientMonitor = new ClientMonitor( advanced, this );
        clientMonitor.read( state );
    }

    // Sizing and placement stuff

    private void updateBlockState()
    {
        getLevel().setBlock( getBlockPos(), getBlockState()
            .setValue( BlockMonitor.STATE, MonitorEdgeState.fromConnections(
                yIndex < height - 1, yIndex > 0,
                xIndex > 0, xIndex < width - 1 ) ), 2 );
    }

    // region Sizing and placement stuff
    public Direction getDirection()
    {
        // Ensure we're actually a monitor block. This _should_ always be the case, but sometimes there's
        // fun problems with the block being missing on the client.
        BlockState state = getBlockState();
        return state.hasProperty( BlockMonitor.FACING ) ? state.getValue( BlockMonitor.FACING ) : Direction.NORTH;
    }

    public Direction getOrientation()
    {
        BlockState state = getBlockState();
        return state.hasProperty( BlockMonitor.ORIENTATION ) ? state.getValue( BlockMonitor.ORIENTATION ) : Direction.NORTH;
    }

    public Direction getFront()
    {
        Direction orientation = getOrientation();
        return orientation == Direction.NORTH ? getDirection() : orientation;
    }

    public Direction getRight()
    {
        return getDirection().getCounterClockWise();
    }

    public Direction getDown()
    {
        Direction orientation = getOrientation();
        if( orientation == Direction.NORTH ) return Direction.UP;
        return orientation == Direction.DOWN ? getDirection() : getDirection().getOpposite();
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getXIndex()
    {
        return xIndex;
    }

    public int getYIndex()
    {
        return yIndex;
    }

    private TileMonitor getSimilarMonitorAt( BlockPos pos )
    {
        if( pos.equals( getBlockPos() ) ) return this;

        int y = pos.getY();
        World world = getLevel();
        if( world == null || !world.isAreaLoaded( pos, 0 ) ) return null;

        TileEntity tile = world.getBlockEntity( pos );
        if( !(tile instanceof TileMonitor) ) return null;

        TileMonitor monitor = (TileMonitor) tile;
        return !monitor.visiting && !monitor.destroyed && advanced == monitor.advanced
            && getDirection() == monitor.getDirection() && getOrientation() == monitor.getOrientation()
            ? monitor : null;
    }

    private TileMonitor getNeighbour( int x, int y )
    {
        BlockPos pos = getBlockPos();
        Direction right = getRight();
        Direction down = getDown();
        int xOffset = -xIndex + x;
        int yOffset = -yIndex + y;
        return getSimilarMonitorAt( pos.relative( right, xOffset ).relative( down, yOffset ) );
    }

    private TileMonitor getOrigin()
    {
        return getNeighbour( 0, 0 );
    }

    private void resize( int width, int height )
    {
        // If we're not already the origin then we'll need to generate a new terminal.
        if( xIndex != 0 || yIndex != 0 ) serverMonitor = null;

        xIndex = 0;
        yIndex = 0;
        this.width = width;
        this.height = height;

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
                if( monitor != null && monitor.peripheral != null )
                {
                    needsTerminal = true;
                    break terminalCheck;
                }
            }
        }

        // Either delete the current monitor or sync a new one.
        if( needsTerminal )
        {
            if( serverMonitor == null ) serverMonitor = new ServerMonitor( advanced, this );
        }
        else
        {
            serverMonitor = null;
        }

        // Update the terminal's width and height and rebuild it. This ensures the monitor
        // is consistent when syncing it to other monitors.
        if( serverMonitor != null ) serverMonitor.rebuild();

        // Update the other monitors, setting coordinates, dimensions and the server terminal
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                TileMonitor monitor = getNeighbour( x, y );
                if( monitor == null ) continue;

                monitor.xIndex = x;
                monitor.yIndex = y;
                monitor.width = width;
                monitor.height = height;
                monitor.serverMonitor = serverMonitor;
                monitor.updateBlockState();
                monitor.updateBlock();
            }
        }
    }

    private boolean mergeLeft()
    {
        TileMonitor left = getNeighbour( -1, 0 );
        if( left == null || left.yIndex != 0 || left.height != height ) return false;

        int width = left.width + this.width;
        if( width > ComputerCraft.monitorWidth ) return false;

        TileMonitor origin = left.getOrigin();
        if( origin != null ) origin.resize( width, height );
        left.expand();
        return true;
    }

    private boolean mergeRight()
    {
        TileMonitor right = getNeighbour( width, 0 );
        if( right == null || right.yIndex != 0 || right.height != height ) return false;

        int width = this.width + right.width;
        if( width > ComputerCraft.monitorWidth ) return false;

        TileMonitor origin = getOrigin();
        if( origin != null ) origin.resize( width, height );
        expand();
        return true;
    }

    private boolean mergeUp()
    {
        TileMonitor above = getNeighbour( 0, height );
        if( above == null || above.xIndex != 0 || above.width != width ) return false;

        int height = above.height + this.height;
        if( height > ComputerCraft.monitorHeight ) return false;

        TileMonitor origin = getOrigin();
        if( origin != null ) origin.resize( width, height );
        expand();
        return true;
    }

    private boolean mergeDown()
    {
        TileMonitor below = getNeighbour( 0, -1 );
        if( below == null || below.xIndex != 0 || below.width != width ) return false;

        int height = this.height + below.height;
        if( height > ComputerCraft.monitorHeight ) return false;

        TileMonitor origin = below.getOrigin();
        if( origin != null ) origin.resize( width, height );
        below.expand();
        return true;
    }

    @SuppressWarnings( "StatementWithEmptyBody" )
    void expand()
    {
        while( mergeLeft() || mergeRight() || mergeUp() || mergeDown() ) ;
    }

    void contractNeighbours()
    {
        visiting = true;
        if( xIndex > 0 )
        {
            TileMonitor left = getNeighbour( xIndex - 1, yIndex );
            if( left != null ) left.contract();
        }
        if( xIndex + 1 < width )
        {
            TileMonitor right = getNeighbour( xIndex + 1, yIndex );
            if( right != null ) right.contract();
        }
        if( yIndex > 0 )
        {
            TileMonitor below = getNeighbour( xIndex, yIndex - 1 );
            if( below != null ) below.contract();
        }
        if( yIndex + 1 < height )
        {
            TileMonitor above = getNeighbour( xIndex, yIndex + 1 );
            if( above != null ) above.contract();
        }
        visiting = false;
    }

    void contract()
    {
        int height = this.height;
        int width = this.width;

        TileMonitor origin = getOrigin();
        if( origin == null )
        {
            TileMonitor right = width > 1 ? getNeighbour( 1, 0 ) : null;
            TileMonitor below = height > 1 ? getNeighbour( 0, 1 ) : null;

            if( right != null ) right.resize( width - 1, 1 );
            if( below != null ) below.resize( width, height - 1 );
            if( right != null ) right.expand();
            if( below != null ) below.expand();

            return;
        }

        for( int y = 0; y < height; y++ )
        {
            for( int x = 0; x < width; x++ )
            {
                TileMonitor monitor = origin.getNeighbour( x, y );
                if( monitor != null ) continue;

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

    private void monitorTouched( float xPos, float yPos, float zPos )
    {
        XYPair pair = XYPair
            .of( xPos, yPos, zPos, getDirection(), getOrientation() )
            .add( xIndex, height - yIndex - 1 );

        if( pair.x > width - RENDER_BORDER || pair.y > height - RENDER_BORDER || pair.x < RENDER_BORDER || pair.y < RENDER_BORDER )
        {
            return;
        }

        ServerTerminal serverTerminal = getServerMonitor();
        if( serverTerminal == null || !serverTerminal.isColour() ) return;

        Terminal originTerminal = serverTerminal.getTerminal();
        if( originTerminal == null ) return;

        double xCharWidth = (width - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getWidth();
        double yCharHeight = (height - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getHeight();

        int xCharPos = (int) Math.min( originTerminal.getWidth(), Math.max( (pair.x - RENDER_BORDER - RENDER_MARGIN) / xCharWidth + 1.0, 1.0 ) );
        int yCharPos = (int) Math.min( originTerminal.getHeight(), Math.max( (pair.y - RENDER_BORDER - RENDER_MARGIN) / yCharHeight + 1.0, 1.0 ) );

        for( int y = 0; y < height; y++ )
        {
            for( int x = 0; x < width; x++ )
            {
                TileMonitor monitor = getNeighbour( x, y );
                if( monitor == null ) continue;

                for( IComputerAccess computer : monitor.computers )
                {
                    computer.queueEvent( "monitor_touch", computer.getAttachmentName(), xCharPos, yCharPos );
                }
            }
        }
    }
    // endregion

    void addComputer( IComputerAccess computer )
    {
        computers.add( computer );
    }

    void removeComputer( IComputerAccess computer )
    {
        computers.remove( computer );
    }

    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        TileMonitor start = getNeighbour( 0, 0 );
        TileMonitor end = getNeighbour( width - 1, height - 1 );
        if( start != null && end != null )
        {
            BlockPos startPos = start.getBlockPos();
            BlockPos endPos = end.getBlockPos();
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
            BlockPos pos = getBlockPos();
            return new AxisAlignedBB( pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1 );
        }
    }

    @Override
    public double getViewDistance()
    {
        return ComputerCraft.monitorDistanceSq;
    }
}
