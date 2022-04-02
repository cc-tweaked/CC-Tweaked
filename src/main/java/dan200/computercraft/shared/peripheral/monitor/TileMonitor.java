/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

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

    private boolean needsUpdate = false;
    private boolean needsValidating = false;
    private boolean destroyed = false;

    // MonitorWatcher state.
    boolean enqueued;
    TerminalState cached;

    private int width = 1;
    private int height = 1;
    private int xIndex = 0;
    private int yIndex = 0;

    private BlockPos bbPos;
    private BlockState bbState;
    private int bbX, bbY, bbWidth, bbHeight;
    private AABB boundingBox;

    public TileMonitor( BlockEntityType<? extends TileMonitor> type, BlockPos pos, BlockState state, boolean advanced )
    {
        super( type, pos, state );
        this.advanced = advanced;
    }

    @Override
    public void clearRemoved() // TODO: Switch back to onLood
    {
        super.clearRemoved();
        needsValidating = true; // Same, tbh
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
    public InteractionResult onActivate( Player player, InteractionHand hand, BlockHitResult hit )
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
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void saveAdditional( CompoundTag tag )
    {
        tag.putInt( NBT_X, xIndex );
        tag.putInt( NBT_Y, yIndex );
        tag.putInt( NBT_WIDTH, width );
        tag.putInt( NBT_HEIGHT, height );
        super.saveAdditional( tag );
    }

    @Override
    public void load( @Nonnull CompoundTag nbt )
    {
        super.load( nbt );

        xIndex = nbt.getInt( NBT_X );
        yIndex = nbt.getInt( NBT_Y );
        width = nbt.getInt( NBT_WIDTH );
        height = nbt.getInt( NBT_HEIGHT );
    }

    @Override
    public void blockTick()
    {
        if( needsValidating )
        {
            needsValidating = false;
            validate();
        }

        if( needsUpdate )
        {
            needsUpdate = false;
            expand();
        }

        if( xIndex != 0 || yIndex != 0 || serverMonitor == null ) return;

        serverMonitor.clearChanged();

        if( serverMonitor.pollResized() ) eachComputer( c -> c.queueEvent( "monitor_resize", c.getAttachmentName() ) );
        if( serverMonitor.pollTerminalChanged() ) MonitorWatcher.enqueue( this );
    }

    @Override
    public void invalidateCaps()
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

    @Nullable
    public ServerMonitor getCachedServerMonitor()
    {
        return serverMonitor;
    }

    @Nullable
    private ServerMonitor getServerMonitor()
    {
        if( serverMonitor != null ) return serverMonitor;

        TileMonitor origin = getOrigin().getMonitor();
        if( origin == null ) return null;

        return serverMonitor = origin.serverMonitor;
    }

    @Nullable
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
                    TileMonitor monitor = getLoadedMonitor( x, y ).getMonitor();
                    if( monitor != null ) monitor.serverMonitor = serverMonitor;
                }
            }

            return serverMonitor;
        }
        else
        {
            // Otherwise fetch the origin and attempt to get its monitor
            // Note this may load chunks, but we don't really have a choice here.
            BlockEntity te = level.getBlockEntity( toWorldPos( 0, 0 ) );
            if( !(te instanceof TileMonitor) ) return null;

            return serverMonitor = ((TileMonitor) te).createServerMonitor();
        }
    }

    @Nullable
    public ClientMonitor getClientMonitor()
    {
        if( clientMonitor != null ) return clientMonitor;

        BlockEntity te = level.getBlockEntity( toWorldPos( 0, 0 ) );
        if( !(te instanceof TileMonitor) ) return null;

        return clientMonitor = ((TileMonitor) te).clientMonitor;
    }

    // Networking stuff

    @Nonnull
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create( this );
    }

    @Nonnull
    @Override
    public final CompoundTag getUpdateTag()
    {
        CompoundTag nbt = super.getUpdateTag();
        nbt.putInt( NBT_X, xIndex );
        nbt.putInt( NBT_Y, yIndex );
        nbt.putInt( NBT_WIDTH, width );
        nbt.putInt( NBT_HEIGHT, height );
        return nbt;
    }

    @Override
    public final void handleUpdateTag( @Nonnull CompoundTag nbt )
    {
        super.handleUpdateTag( nbt );

        int oldXIndex = xIndex;
        int oldYIndex = yIndex;

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

    boolean isCompatible( TileMonitor other )
    {
        return !other.destroyed && advanced == other.advanced && getOrientation() == other.getOrientation() && getDirection() == other.getDirection();
    }

    /**
     * Get a tile within the current monitor only if it is loaded and compatible.
     *
     * @param x Absolute X position in monitor coordinates
     * @param y Absolute Y position in monitor coordinates
     * @return The located monitor
     */
    @Nonnull
    private MonitorState getLoadedMonitor( int x, int y )
    {
        if( x == xIndex && y == yIndex ) return MonitorState.present( this );
        BlockPos pos = toWorldPos( x, y );

        Level world = getLevel();
        if( world == null || !world.isLoaded( pos ) ) return MonitorState.UNLOADED;

        BlockEntity tile = world.getBlockEntity( pos );
        if( !(tile instanceof TileMonitor monitor) ) return MonitorState.MISSING;

        return isCompatible( monitor ) ? MonitorState.present( monitor ) : MonitorState.MISSING;
    }

    private MonitorState getOrigin()
    {
        return getLoadedMonitor( 0, 0 );
    }

    /**
     * Convert monitor coordinates to world coordinates.
     *
     * @param x Absolute X position in monitor coordinates
     * @param y Absolute Y position in monitor coordinates
     * @return The monitor's position.
     */
    BlockPos toWorldPos( int x, int y )
    {
        if( xIndex == x && yIndex == y ) return getBlockPos();
        return getBlockPos().relative( getRight(), -xIndex + x ).relative( getDown(), -yIndex + y );
    }

    void resize( int width, int height )
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
                TileMonitor monitor = getLoadedMonitor( x, y ).getMonitor();
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
        BlockPos pos = getBlockPos();
        Direction down = getDown(), right = getRight();
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                BlockEntity other = getLevel().getBlockEntity( pos.relative( right, x ).relative( down, y ) );
                if( !(other instanceof TileMonitor monitor) || !isCompatible( monitor ) ) continue;

                monitor.xIndex = x;
                monitor.yIndex = y;
                monitor.width = width;
                monitor.height = height;
                monitor.serverMonitor = serverMonitor;
                monitor.needsUpdate = monitor.needsValidating = false;
                monitor.updateBlockState();
                monitor.updateBlock();
            }
        }
    }

    void updateNeighborsDeferred()
    {
        needsUpdate = true;
    }

    void expand()
    {
        TileMonitor monitor = getOrigin().getMonitor();
        if( monitor != null && monitor.xIndex == 0 && monitor.yIndex == 0 ) new Expander( monitor ).expand();
    }

    private void contractNeighbours()
    {
        if( width == 1 && height == 1 ) return;

        BlockPos pos = getBlockPos();
        Direction down = getDown(), right = getRight();
        BlockPos origin = toWorldPos( 0, 0 );

        TileMonitor toLeft = null, toAbove = null, toRight = null, toBelow = null;
        if( xIndex > 0 ) toLeft = tryResizeAt( pos.relative( right, -xIndex ), xIndex, 1 );
        if( yIndex > 0 ) toAbove = tryResizeAt( origin, width, yIndex );
        if( xIndex < width - 1 ) toRight = tryResizeAt( pos.relative( right, 1 ), width - xIndex - 1, 1 );
        if( yIndex < height - 1 )
        {
            toBelow = tryResizeAt( origin.relative( down, yIndex + 1 ), width, height - yIndex - 1 );
        }

        if( toLeft != null ) toLeft.expand();
        if( toAbove != null ) toAbove.expand();
        if( toRight != null ) toRight.expand();
        if( toBelow != null ) toBelow.expand();
    }

    @Nullable
    private TileMonitor tryResizeAt( BlockPos pos, int width, int height )
    {
        BlockEntity tile = level.getBlockEntity( pos );
        if( tile instanceof TileMonitor monitor && isCompatible( monitor ) )
        {
            monitor.resize( width, height );
            return monitor;
        }

        return null;
    }


    private boolean checkMonitorAt( int xIndex, int yIndex )
    {
        MonitorState state = getLoadedMonitor( xIndex, yIndex );
        if( state.isMissing() ) return false;

        TileMonitor monitor = state.getMonitor();
        if( monitor == null ) return true;

        return monitor.xIndex == xIndex && monitor.yIndex == yIndex && monitor.width == width && monitor.height == height;
    }

    private void validate()
    {
        if( xIndex == 0 && yIndex == 0 && width == 1 && height == 1 ) return;

        if( xIndex >= 0 && xIndex <= width && width > 0 && width <= ComputerCraft.monitorWidth &&
            yIndex >= 0 && yIndex <= height && height > 0 && height <= ComputerCraft.monitorHeight &&
            checkMonitorAt( 0, 0 ) && checkMonitorAt( 0, height - 1 ) &&
            checkMonitorAt( width - 1, 0 ) && checkMonitorAt( width - 1, height - 1 ) )
        {
            return;
        }

        // Something in our monitor is invalid. For now, let's just reset ourselves and then try to integrate ourselves
        // later.
        ComputerCraft.log.warn( "Monitor is malformed, resetting to 1x1." );
        resize( 1, 1 );
        needsUpdate = true;
    }
    // endregion

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

        eachComputer( c -> c.queueEvent( "monitor_touch", c.getAttachmentName(), xCharPos, yCharPos ) );
    }

    private void eachComputer( Consumer<IComputerAccess> fun )
    {
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                TileMonitor monitor = getLoadedMonitor( x, y ).getMonitor();
                if( monitor == null ) continue;

                for( IComputerAccess computer : monitor.computers ) fun.accept( computer );
            }
        }
    }

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
    public AABB getRenderBoundingBox()
    {
        // We attempt to cache the bounding box to save having to do property lookups (and allocations!) on every frame.
        // Unfortunately the AABB does depend on quite a lot of state, so we need to add a bunch of extra fields -
        // ideally these'd be a single object, but I don't think worth doing until Java has value types.
        if( boundingBox != null && getBlockState().equals( bbState ) && getBlockPos().equals( bbPos ) &&
            xIndex == bbX && yIndex == bbY && width == bbWidth && height == bbHeight )
        {
            return boundingBox;
        }

        bbState = getBlockState();
        bbPos = getBlockPos();
        bbX = xIndex;
        bbY = yIndex;
        bbWidth = width;
        bbHeight = height;

        BlockPos startPos = toWorldPos( 0, 0 );
        BlockPos endPos = toWorldPos( width, height );
        return boundingBox = new AABB(
            Math.min( startPos.getX(), endPos.getX() ),
            Math.min( startPos.getY(), endPos.getY() ),
            Math.min( startPos.getZ(), endPos.getZ() ),
            Math.max( startPos.getX(), endPos.getX() ) + 1,
            Math.max( startPos.getY(), endPos.getY() ) + 1,
            Math.max( startPos.getZ(), endPos.getZ() ) + 1
        );
    }
}
