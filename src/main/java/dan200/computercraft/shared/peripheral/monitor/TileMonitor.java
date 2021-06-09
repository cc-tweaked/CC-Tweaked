/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.network.client.TerminalState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class TileMonitor extends TileGeneric implements IPeripheralTile
{
    public static final double RENDER_BORDER = 2.0 / 16.0;
    public static final double RENDER_MARGIN = 0.5 / 16.0;
    public static final double RENDER_PIXEL_SCALE = 1.0 / 64.0;

    private static final String NBT_X = "XIndex";
    private static final String NBT_Y = "YIndex";
    private static final String NBT_WIDTH = "Width";
    private static final String NBT_HEIGHT = "Height";

    private final boolean advanced;
    private final Set<IComputerAccess> computers = new HashSet<>();
    // MonitorWatcher state.
    boolean enqueued;
    TerminalState cached;
    private ServerMonitor serverMonitor;
    private ClientMonitor clientMonitor;
    private MonitorPeripheral peripheral;
    private boolean needsUpdate = false;
    private boolean destroyed = false;
    private boolean visiting = false;
    private int width = 1;
    private int height = 1;
    private int xIndex = 0;
    private int yIndex = 0;

    public TileMonitor( BlockEntityType<? extends TileMonitor> type, boolean advanced )
    {
        super( type );
        this.advanced = advanced;
    }

    @Override
    public void destroy()
    {
        // TODO: Call this before using the block
        if( this.destroyed )
        {
            return;
        }
        this.destroyed = true;
        if( !this.getWorld().isClient )
        {
            this.contractNeighbours();
        }
    }

    @Override
    public void markRemoved()
    {
        super.markRemoved();
        if( this.clientMonitor != null && this.xIndex == 0 && this.yIndex == 0 )
        {
            this.clientMonitor.destroy();
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        if( this.clientMonitor != null && this.xIndex == 0 && this.yIndex == 0 )
        {
            this.clientMonitor.destroy();
        }
        this.clientMonitor = null;
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( !player.isInSneakingPose() && this.getFront() == hit.getSide() )
        {
            if( !this.getWorld().isClient )
            {
                this.monitorTouched( (float) (hit.getPos().x - hit.getBlockPos()
                        .getX()),
                    (float) (hit.getPos().y - hit.getBlockPos()
                        .getY()),
                    (float) (hit.getPos().z - hit.getBlockPos()
                        .getZ()) );
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void blockTick()
    {
        if( needsUpdate )
        {
            needsUpdate = false;
            updateNeighbors();
        }

        if( this.xIndex != 0 || this.yIndex != 0 || this.serverMonitor == null )
        {
            return;
        }

        this.serverMonitor.clearChanged();

        if( this.serverMonitor.pollResized() )
        {
            for( int x = 0; x < this.width; x++ )
            {
                for( int y = 0; y < this.height; y++ )
                {
                    TileMonitor monitor = this.getNeighbour( x, y );
                    if( monitor == null )
                    {
                        continue;
                    }

                    for( IComputerAccess computer : monitor.computers )
                    {
                        computer.queueEvent( "monitor_resize", computer.getAttachmentName() );
                    }
                }
            }
        }

        if( this.serverMonitor.pollTerminalChanged() )
        {
            this.updateBlock();
        }
    }

    @Override
    protected final void readDescription( @Nonnull CompoundTag nbt )
    {
        super.readDescription( nbt );

        int oldXIndex = this.xIndex;
        int oldYIndex = this.yIndex;
        int oldWidth = this.width;
        int oldHeight = this.height;

        this.xIndex = nbt.getInt( NBT_X );
        this.yIndex = nbt.getInt( NBT_Y );
        this.width = nbt.getInt( NBT_WIDTH );
        this.height = nbt.getInt( NBT_HEIGHT );

        if( oldXIndex != this.xIndex || oldYIndex != this.yIndex )
        {
            // If our index has changed then it's possible the origin monitor has changed. Thus
            // we'll clear our cache. If we're the origin then we'll need to remove the glList as well.
            if( oldXIndex == 0 && oldYIndex == 0 && this.clientMonitor != null )
            {
                this.clientMonitor.destroy();
            }
            this.clientMonitor = null;
        }

        if( this.xIndex == 0 && this.yIndex == 0 )
        {
            // If we're the origin terminal then create it.
            if( this.clientMonitor == null )
            {
                this.clientMonitor = new ClientMonitor( this.advanced, this );
            }
            this.clientMonitor.readDescription( nbt );
        }

        if( oldXIndex != this.xIndex || oldYIndex != this.yIndex || oldWidth != this.width || oldHeight != this.height )
        {
            // One of our properties has changed, so ensure we redraw the block
            this.updateBlock();
        }
    }

    @Override
    protected void writeDescription( @Nonnull CompoundTag nbt )
    {
        super.writeDescription( nbt );
        nbt.putInt( NBT_X, this.xIndex );
        nbt.putInt( NBT_Y, this.yIndex );
        nbt.putInt( NBT_WIDTH, this.width );
        nbt.putInt( NBT_HEIGHT, this.height );

        if( this.xIndex == 0 && this.yIndex == 0 && this.serverMonitor != null )
        {
            this.serverMonitor.writeDescription( nbt );
        }
    }

    private TileMonitor getNeighbour( int x, int y )
    {
        BlockPos pos = this.getPos();
        Direction right = this.getRight();
        Direction down = this.getDown();
        int xOffset = -this.xIndex + x;
        int yOffset = -this.yIndex + y;
        return this.getSimilarMonitorAt( pos.offset( right, xOffset )
            .offset( down, yOffset ) );
    }

    public Direction getRight()
    {
        return this.getDirection().rotateYCounterclockwise();
    }

    public Direction getDown()
    {
        Direction orientation = this.getOrientation();
        if( orientation == Direction.NORTH )
        {
            return Direction.UP;
        }
        return orientation == Direction.DOWN ? this.getDirection() : this.getDirection().getOpposite();
    }

    private TileMonitor getSimilarMonitorAt( BlockPos pos )
    {
        if( pos.equals( this.getPos() ) )
        {
            return this;
        }

        int y = pos.getY();
        World world = this.getWorld();
        if( world == null || !world.isChunkLoaded( pos ) )
        {
            return null;
        }

        BlockEntity tile = world.getBlockEntity( pos );
        if( !(tile instanceof TileMonitor) )
        {
            return null;
        }

        TileMonitor monitor = (TileMonitor) tile;
        return !monitor.visiting && !monitor.destroyed && this.advanced == monitor.advanced && this.getDirection() == monitor.getDirection() && this.getOrientation() == monitor.getOrientation() ? monitor : null;
    }

    // region Sizing and placement stuff
    public Direction getDirection()
    {
        // Ensure we're actually a monitor block. This _should_ always be the case, but sometimes there's
        // fun problems with the block being missing on the client.
        BlockState state = getCachedState();
        return state.contains( BlockMonitor.FACING ) ? state.get( BlockMonitor.FACING ) : Direction.NORTH;
    }

    public Direction getOrientation()
    {
        return this.getCachedState().get( BlockMonitor.ORIENTATION );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );

        this.xIndex = nbt.getInt( NBT_X );
        this.yIndex = nbt.getInt( NBT_Y );
        this.width = nbt.getInt( NBT_WIDTH );
        this.height = nbt.getInt( NBT_HEIGHT );
    }

    // Networking stuff

    @Nonnull
    @Override
    public CompoundTag toTag( CompoundTag tag )
    {
        tag.putInt( NBT_X, this.xIndex );
        tag.putInt( NBT_Y, this.yIndex );
        tag.putInt( NBT_WIDTH, this.width );
        tag.putInt( NBT_HEIGHT, this.height );
        return super.toTag( tag );
    }

    @Override
    public double getRenderDistance()
    {
        return ComputerCraft.monitorDistanceSq;
    }

    // Sizing and placement stuff

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval();
        TickScheduler.schedule( this );
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        this.createServerMonitor(); // Ensure the monitor is created before doing anything else.
        if( this.peripheral == null )
        {
            this.peripheral = new MonitorPeripheral( this );
        }
        return this.peripheral;
    }

    public ServerMonitor getCachedServerMonitor()
    {
        return this.serverMonitor;
    }

    private ServerMonitor getServerMonitor()
    {
        if( this.serverMonitor != null )
        {
            return this.serverMonitor;
        }

        TileMonitor origin = this.getOrigin();
        if( origin == null )
        {
            return null;
        }

        return this.serverMonitor = origin.serverMonitor;
    }

    private ServerMonitor createServerMonitor()
    {
        if( this.serverMonitor != null )
        {
            return this.serverMonitor;
        }

        if( this.xIndex == 0 && this.yIndex == 0 )
        {
            // If we're the origin, set up the new monitor
            this.serverMonitor = new ServerMonitor( this.advanced, this );
            this.serverMonitor.rebuild();

            // And propagate it to child monitors
            for( int x = 0; x < this.width; x++ )
            {
                for( int y = 0; y < this.height; y++ )
                {
                    TileMonitor monitor = this.getNeighbour( x, y );
                    if( monitor != null )
                    {
                        monitor.serverMonitor = this.serverMonitor;
                    }
                }
            }

            return this.serverMonitor;
        }
        else
        {
            // Otherwise fetch the origin and attempt to get its monitor
            // Note this may load chunks, but we don't really have a choice here.
            BlockPos pos = this.getPos();
            BlockEntity te = this.world.getBlockEntity( pos.offset( this.getRight(), -this.xIndex )
                .offset( this.getDown(), -this.yIndex ) );
            if( !(te instanceof TileMonitor) )
            {
                return null;
            }

            return this.serverMonitor = ((TileMonitor) te).createServerMonitor();
        }
    }

    public ClientMonitor getClientMonitor()
    {
        if( this.clientMonitor != null )
        {
            return this.clientMonitor;
        }

        BlockPos pos = this.getPos();
        BlockEntity te = this.world.getBlockEntity( pos.offset( this.getRight(), -this.xIndex )
            .offset( this.getDown(), -this.yIndex ) );
        if( !(te instanceof TileMonitor) )
        {
            return null;
        }

        return this.clientMonitor = ((TileMonitor) te).clientMonitor;
    }

    public final void read( TerminalState state )
    {
        if( this.xIndex != 0 || this.yIndex != 0 )
        {
            ComputerCraft.log.warn( "Receiving monitor state for non-origin terminal at {}", this.getPos() );
            return;
        }

        if( this.clientMonitor == null )
        {
            this.clientMonitor = new ClientMonitor( this.advanced, this );
        }
        this.clientMonitor.read( state );
    }

    private void updateBlockState()
    {
        this.getWorld().setBlockState( this.getPos(),
            this.getCachedState().with( BlockMonitor.STATE,
                MonitorEdgeState.fromConnections( this.yIndex < this.height - 1,
                    this.yIndex > 0, this.xIndex > 0, this.xIndex < this.width - 1 ) ),
            2 );
    }

    public Direction getFront()
    {
        Direction orientation = this.getOrientation();
        return orientation == Direction.NORTH ? this.getDirection() : orientation;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public int getXIndex()
    {
        return this.xIndex;
    }

    public int getYIndex()
    {
        return this.yIndex;
    }

    private TileMonitor getOrigin()
    {
        return this.getNeighbour( 0, 0 );
    }

    private void resize( int width, int height )
    {
        // If we're not already the origin then we'll need to generate a new terminal.
        if( this.xIndex != 0 || this.yIndex != 0 )
        {
            this.serverMonitor = null;
        }

        this.xIndex = 0;
        this.yIndex = 0;
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
                TileMonitor monitor = this.getNeighbour( x, y );
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
            if( this.serverMonitor == null )
            {
                this.serverMonitor = new ServerMonitor( this.advanced, this );
            }
        }
        else
        {
            this.serverMonitor = null;
        }

        // Update the terminal's width and height and rebuild it. This ensures the monitor
        // is consistent when syncing it to other monitors.
        if( this.serverMonitor != null )
        {
            this.serverMonitor.rebuild();
        }

        // Update the other monitors, setting coordinates, dimensions and the server terminal
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                TileMonitor monitor = this.getNeighbour( x, y );
                if( monitor == null )
                {
                    continue;
                }

                monitor.xIndex = x;
                monitor.yIndex = y;
                monitor.width = width;
                monitor.height = height;
                monitor.serverMonitor = this.serverMonitor;
                monitor.updateBlockState();
                monitor.updateBlock();
            }
        }
    }

    private boolean mergeLeft()
    {
        TileMonitor left = this.getNeighbour( -1, 0 );
        if( left == null || left.yIndex != 0 || left.height != this.height )
        {
            return false;
        }

        int width = left.width + this.width;
        if( width > ComputerCraft.monitorWidth )
        {
            return false;
        }

        TileMonitor origin = left.getOrigin();
        if( origin != null )
        {
            origin.resize( width, this.height );
        }
        left.expand();
        return true;
    }

    private boolean mergeRight()
    {
        TileMonitor right = this.getNeighbour( this.width, 0 );
        if( right == null || right.yIndex != 0 || right.height != this.height )
        {
            return false;
        }

        int width = this.width + right.width;
        if( width > ComputerCraft.monitorWidth )
        {
            return false;
        }

        TileMonitor origin = this.getOrigin();
        if( origin != null )
        {
            origin.resize( width, this.height );
        }
        this.expand();
        return true;
    }

    private boolean mergeUp()
    {
        TileMonitor above = this.getNeighbour( 0, this.height );
        if( above == null || above.xIndex != 0 || above.width != this.width )
        {
            return false;
        }

        int height = above.height + this.height;
        if( height > ComputerCraft.monitorHeight )
        {
            return false;
        }

        TileMonitor origin = this.getOrigin();
        if( origin != null )
        {
            origin.resize( this.width, height );
        }
        this.expand();
        return true;
    }

    private boolean mergeDown()
    {
        TileMonitor below = this.getNeighbour( 0, -1 );
        if( below == null || below.xIndex != 0 || below.width != this.width )
        {
            return false;
        }

        int height = this.height + below.height;
        if( height > ComputerCraft.monitorHeight )
        {
            return false;
        }

        TileMonitor origin = below.getOrigin();
        if( origin != null )
        {
            origin.resize( this.width, height );
        }
        below.expand();
        return true;
    }

    void updateNeighborsDeferred()
    {
        needsUpdate = true;
    }

    void updateNeighbors()
    {
        contractNeighbours();
        contract();
        expand();
    }

    @SuppressWarnings( "StatementWithEmptyBody" )
    void expand()
    {
        while( this.mergeLeft() || this.mergeRight() || this.mergeUp() || this.mergeDown() )
        {
        }
    }

    void contractNeighbours()
    {
        this.visiting = true;
        if( this.xIndex > 0 )
        {
            TileMonitor left = this.getNeighbour( this.xIndex - 1, this.yIndex );
            if( left != null )
            {
                left.contract();
            }
        }
        if( this.xIndex + 1 < this.width )
        {
            TileMonitor right = this.getNeighbour( this.xIndex + 1, this.yIndex );
            if( right != null )
            {
                right.contract();
            }
        }
        if( this.yIndex > 0 )
        {
            TileMonitor below = this.getNeighbour( this.xIndex, this.yIndex - 1 );
            if( below != null )
            {
                below.contract();
            }
        }
        if( this.yIndex + 1 < this.height )
        {
            TileMonitor above = this.getNeighbour( this.xIndex, this.yIndex + 1 );
            if( above != null )
            {
                above.contract();
            }
        }
        this.visiting = false;
    }

    void contract()
    {
        int height = this.height;
        int width = this.width;

        TileMonitor origin = this.getOrigin();
        if( origin == null )
        {
            TileMonitor right = width > 1 ? this.getNeighbour( 1, 0 ) : null;
            TileMonitor below = height > 1 ? this.getNeighbour( 0, 1 ) : null;

            if( right != null )
            {
                right.resize( width - 1, 1 );
            }
            if( below != null )
            {
                below.resize( width, height - 1 );
            }
            if( right != null )
            {
                right.expand();
            }
            if( below != null )
            {
                below.expand();
            }

            return;
        }

        for( int y = 0; y < height; y++ )
        {
            for( int x = 0; x < width; x++ )
            {
                TileMonitor monitor = origin.getNeighbour( x, y );
                if( monitor != null )
                {
                    continue;
                }

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
                if( above != null )
                {
                    above.expand();
                }
                if( left != null )
                {
                    left.expand();
                }
                if( right != null )
                {
                    right.expand();
                }
                if( below != null )
                {
                    below.expand();
                }
                return;
            }
        }
    }
    // endregion

    private void monitorTouched( float xPos, float yPos, float zPos )
    {
        XYPair pair = XYPair.of( xPos, yPos, zPos, this.getDirection(), this.getOrientation() )
            .add( this.xIndex, this.height - this.yIndex - 1 );

        if( pair.x > this.width - RENDER_BORDER || pair.y > this.height - RENDER_BORDER || pair.x < RENDER_BORDER || pair.y < RENDER_BORDER )
        {
            return;
        }

        ServerTerminal serverTerminal = this.getServerMonitor();
        if( serverTerminal == null || !serverTerminal.isColour() )
        {
            return;
        }

        Terminal originTerminal = serverTerminal.getTerminal();
        if( originTerminal == null )
        {
            return;
        }

        double xCharWidth = (this.width - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getWidth();
        double yCharHeight = (this.height - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getHeight();

        int xCharPos = (int) Math.min( originTerminal.getWidth(), Math.max( (pair.x - RENDER_BORDER - RENDER_MARGIN) / xCharWidth + 1.0, 1.0 ) );
        int yCharPos = (int) Math.min( originTerminal.getHeight(), Math.max( (pair.y - RENDER_BORDER - RENDER_MARGIN) / yCharHeight + 1.0, 1.0 ) );

        for( int y = 0; y < this.height; y++ )
        {
            for( int x = 0; x < this.width; x++ )
            {
                TileMonitor monitor = this.getNeighbour( x, y );
                if( monitor == null )
                {
                    continue;
                }

                for( IComputerAccess computer : monitor.computers )
                {
                    computer.queueEvent( "monitor_touch", computer.getAttachmentName(), xCharPos, yCharPos );
                }
            }
        }
    }

    void addComputer( IComputerAccess computer )
    {
        this.computers.add( computer );
    }

    //    @Nonnull
    //    @Override
    //    public Box getRenderBoundingBox()
    //    {
    //        TileMonitor start = getNeighbour( 0, 0 );
    //        TileMonitor end = getNeighbour( m_width - 1, m_height - 1 );
    //        if( start != null && end != null )
    //        {
    //            BlockPos startPos = start.getPos();
    //            BlockPos endPos = end.getPos();
    //            int minX = Math.min( startPos.getX(), endPos.getX() );
    //            int minY = Math.min( startPos.getY(), endPos.getY() );
    //            int minZ = Math.min( startPos.getZ(), endPos.getZ() );
    //            int maxX = Math.max( startPos.getX(), endPos.getX() ) + 1;
    //            int maxY = Math.max( startPos.getY(), endPos.getY() ) + 1;
    //            int maxZ = Math.max( startPos.getZ(), endPos.getZ() ) + 1;
    //            return new Box( minX, minY, minZ, maxX, maxY, maxZ );
    //        }
    //        else
    //        {
    //            BlockPos pos = getPos();
    //            return new Box( pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1 );
    //        }
    //    }

    void removeComputer( IComputerAccess computer )
    {
        this.computers.remove( computer );
    }
}
