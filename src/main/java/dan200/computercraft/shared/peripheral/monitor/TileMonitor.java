/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class TileMonitor extends TileGeneric implements IPeripheralTile {
    public static final double RENDER_BORDER = 2.0 / 16.0;
    public static final double RENDER_MARGIN = 0.5 / 16.0;
    public static final double RENDER_PIXEL_SCALE = 1.0 / 64.0;

    private static final String NBT_X = "XIndex";
    private static final String NBT_Y = "YIndex";
    private static final String NBT_WIDTH = "Width";
    private static final String NBT_HEIGHT = "Height";

    private final boolean advanced;
    private final Set<IComputerAccess> m_computers = new HashSet<>();
    // MonitorWatcher state.
    boolean enqueued;
    TerminalState cached;
    private ServerMonitor m_serverMonitor;
    private ClientMonitor m_clientMonitor;
    private MonitorPeripheral peripheral;
    private boolean m_destroyed = false;
    private boolean visiting = false;
    private int m_width = 1;
    private int m_height = 1;
    private int m_xIndex = 0;
    private int m_yIndex = 0;

    public TileMonitor(BlockEntityType<? extends TileMonitor> type, boolean advanced) {
        super(type);
        this.advanced = advanced;
    }

    @Override
    public void destroy() {
        // TODO: Call this before using the block
        if (this.m_destroyed) {
            return;
        }
        this.m_destroyed = true;
        if (!this.getWorld().isClient) {
            this.contractNeighbours();
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.isInSneakingPose() && this.getFront() == hit.getSide()) {
            if (!this.getWorld().isClient) {
                this.monitorTouched((float) (hit.getPos().x - hit.getBlockPos()
                                                                 .getX()),
                                    (float) (hit.getPos().y - hit.getBlockPos()
                                                            .getY()),
                                    (float) (hit.getPos().z - hit.getBlockPos()
                                                            .getZ()));
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void blockTick() {
        if (this.m_xIndex != 0 || this.m_yIndex != 0 || this.m_serverMonitor == null) {
            return;
        }

        this.m_serverMonitor.clearChanged();

        if (this.m_serverMonitor.pollResized()) {
            for (int x = 0; x < this.m_width; x++) {
                for (int y = 0; y < this.m_height; y++) {
                    TileMonitor monitor = this.getNeighbour(x, y);
                    if (monitor == null) {
                        continue;
                    }

                    for (IComputerAccess computer : monitor.m_computers) {
                        computer.queueEvent("monitor_resize", computer.getAttachmentName());
                    }
                }
            }
        }

        if (this.m_serverMonitor.pollTerminalChanged()) {
            this.updateBlock();
        }
    }

    @Override
    protected final void readDescription(@Nonnull CompoundTag nbt) {
        super.readDescription(nbt);

        int oldXIndex = this.m_xIndex;
        int oldYIndex = this.m_yIndex;
        int oldWidth = this.m_width;
        int oldHeight = this.m_height;

        this.m_xIndex = nbt.getInt(NBT_X);
        this.m_yIndex = nbt.getInt(NBT_Y);
        this.m_width = nbt.getInt(NBT_WIDTH);
        this.m_height = nbt.getInt(NBT_HEIGHT);

        if (oldXIndex != this.m_xIndex || oldYIndex != this.m_yIndex) {
            // If our index has changed then it's possible the origin monitor has changed. Thus
            // we'll clear our cache. If we're the origin then we'll need to remove the glList as well.
            if (oldXIndex == 0 && oldYIndex == 0 && this.m_clientMonitor != null) {
                this.m_clientMonitor.destroy();
            }
            this.m_clientMonitor = null;
        }

        if (this.m_xIndex == 0 && this.m_yIndex == 0) {
            // If we're the origin terminal then create it.
            if (this.m_clientMonitor == null) {
                this.m_clientMonitor = new ClientMonitor(this.advanced, this);
            }
            this.m_clientMonitor.readDescription(nbt);
        }

        if (oldXIndex != this.m_xIndex || oldYIndex != this.m_yIndex || oldWidth != this.m_width || oldHeight != this.m_height) {
            // One of our properties has changed, so ensure we redraw the block
            this.updateBlock();
        }
    }

    @Override
    protected void writeDescription(@Nonnull CompoundTag nbt) {
        super.writeDescription(nbt);
        nbt.putInt(NBT_X, this.m_xIndex);
        nbt.putInt(NBT_Y, this.m_yIndex);
        nbt.putInt(NBT_WIDTH, this.m_width);
        nbt.putInt(NBT_HEIGHT, this.m_height);

        if (this.m_xIndex == 0 && this.m_yIndex == 0 && this.m_serverMonitor != null) {
            this.m_serverMonitor.writeDescription(nbt);
        }
    }

    private TileMonitor getNeighbour(int x, int y) {
        BlockPos pos = this.getPos();
        Direction right = this.getRight();
        Direction down = this.getDown();
        int xOffset = -this.m_xIndex + x;
        int yOffset = -this.m_yIndex + y;
        return this.getSimilarMonitorAt(pos.offset(right, xOffset)
                                           .offset(down, yOffset));
    }

    public Direction getRight() {
        return this.getDirection().rotateYCounterclockwise();
    }

    public Direction getDown() {
        Direction orientation = this.getOrientation();
        if (orientation == Direction.NORTH) {
            return Direction.UP;
        }
        return orientation == Direction.DOWN ? this.getDirection() : this.getDirection().getOpposite();
    }

    private TileMonitor getSimilarMonitorAt(BlockPos pos) {
        if (pos.equals(this.getPos())) {
            return this;
        }

        int y = pos.getY();
        World world = this.getWorld();
        if (world == null || !world.isChunkLoaded(pos)) {
            return null;
        }

        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof TileMonitor)) {
            return null;
        }

        TileMonitor monitor = (TileMonitor) tile;
        return !monitor.visiting && !monitor.m_destroyed && this.advanced == monitor.advanced && this.getDirection() == monitor.getDirection() && this.getOrientation() == monitor.getOrientation() ? monitor : null;
    }

    // region Sizing and placement stuff
    public Direction getDirection() {
        // Ensure we're actually a monitor block. This _should_ always be the case, but sometimes there's
        // fun problems with the block being missing on the client.
        BlockState state = getCachedState();
        return state.contains( BlockMonitor.FACING ) ? state.get( BlockMonitor.FACING ) : Direction.NORTH;
    }

    public Direction getOrientation() {
        return this.getCachedState().get(BlockMonitor.ORIENTATION);
    }

    @Override
    public void fromTag(@Nonnull BlockState state, @Nonnull CompoundTag nbt) {
        super.fromTag(state, nbt);

        this.m_xIndex = nbt.getInt(NBT_X);
        this.m_yIndex = nbt.getInt(NBT_Y);
        this.m_width = nbt.getInt(NBT_WIDTH);
        this.m_height = nbt.getInt(NBT_HEIGHT);
    }

    // Networking stuff

    @Nonnull
    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag.putInt(NBT_X, this.m_xIndex);
        tag.putInt(NBT_Y, this.m_yIndex);
        tag.putInt(NBT_WIDTH, this.m_width);
        tag.putInt(NBT_HEIGHT, this.m_height);
        return super.toTag(tag);
    }

    @Override
    public double getSquaredRenderDistance() {
        return ComputerCraft.monitorDistanceSq;
    }

    @Override
    @Environment (EnvType.CLIENT)
    public void markRemoved() {
        super.markRemoved();
        if (this.m_clientMonitor != null && this.m_xIndex == 0 && this.m_yIndex == 0) {
            this.m_clientMonitor.destroy();
        }
    }

    // Sizing and placement stuff

    @Override
    public void cancelRemoval() {
        super.cancelRemoval();
        TickScheduler.schedule(this);
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        this.createServerMonitor(); // Ensure the monitor is created before doing anything else.
        if (this.peripheral == null) {
            this.peripheral = new MonitorPeripheral(this);
        }
        return this.peripheral;
    }

    public ServerMonitor getCachedServerMonitor() {
        return this.m_serverMonitor;
    }

    private ServerMonitor getServerMonitor() {
        if (this.m_serverMonitor != null) {
            return this.m_serverMonitor;
        }

        TileMonitor origin = this.getOrigin();
        if (origin == null) {
            return null;
        }

        return this.m_serverMonitor = origin.m_serverMonitor;
    }

    private ServerMonitor createServerMonitor() {
        if (this.m_serverMonitor != null) {
            return this.m_serverMonitor;
        }

        if (this.m_xIndex == 0 && this.m_yIndex == 0) {
            // If we're the origin, set up the new monitor
            this.m_serverMonitor = new ServerMonitor(this.advanced, this);
            this.m_serverMonitor.rebuild();

            // And propagate it to child monitors
            for (int x = 0; x < this.m_width; x++) {
                for (int y = 0; y < this.m_height; y++) {
                    TileMonitor monitor = this.getNeighbour(x, y);
                    if (monitor != null) {
                        monitor.m_serverMonitor = this.m_serverMonitor;
                    }
                }
            }

            return this.m_serverMonitor;
        } else {
            // Otherwise fetch the origin and attempt to get its monitor
            // Note this may load chunks, but we don't really have a choice here.
            BlockPos pos = this.getPos();
            BlockEntity te = this.world.getBlockEntity(pos.offset(this.getRight(), -this.m_xIndex)
                                                          .offset(this.getDown(), -this.m_yIndex));
            if (!(te instanceof TileMonitor)) {
                return null;
            }

            return this.m_serverMonitor = ((TileMonitor) te).createServerMonitor();
        }
    }

    public ClientMonitor getClientMonitor() {
        if (this.m_clientMonitor != null) {
            return this.m_clientMonitor;
        }

        BlockPos pos = this.getPos();
        BlockEntity te = this.world.getBlockEntity(pos.offset(this.getRight(), -this.m_xIndex)
                                                      .offset(this.getDown(), -this.m_yIndex));
        if (!(te instanceof TileMonitor)) {
            return null;
        }

        return this.m_clientMonitor = ((TileMonitor) te).m_clientMonitor;
    }

    public final void read(TerminalState state) {
        if (this.m_xIndex != 0 || this.m_yIndex != 0) {
            ComputerCraft.log.warn("Receiving monitor state for non-origin terminal at {}", this.getPos());
            return;
        }

        if (this.m_clientMonitor == null) {
            this.m_clientMonitor = new ClientMonitor(this.advanced, this);
        }
        this.m_clientMonitor.read(state);
    }

    private void updateBlockState() {
        this.getWorld().setBlockState(this.getPos(),
                                      this.getCachedState().with(BlockMonitor.STATE,
                                                            MonitorEdgeState.fromConnections(this.m_yIndex < this.m_height - 1,
                                                                                        this.m_yIndex > 0, this.m_xIndex > 0, this.m_xIndex < this.m_width - 1)),
                                      2);
    }

    public Direction getFront() {
        Direction orientation = this.getOrientation();
        return orientation == Direction.NORTH ? this.getDirection() : orientation;
    }

    public int getWidth() {
        return this.m_width;
    }

    public int getHeight() {
        return this.m_height;
    }

    public int getXIndex() {
        return this.m_xIndex;
    }

    public int getYIndex() {
        return this.m_yIndex;
    }

    private TileMonitor getOrigin() {
        return this.getNeighbour(0, 0);
    }

    private void resize(int width, int height) {
        // If we're not already the origin then we'll need to generate a new terminal.
        if (this.m_xIndex != 0 || this.m_yIndex != 0) {
            this.m_serverMonitor = null;
        }

        this.m_xIndex = 0;
        this.m_yIndex = 0;
        this.m_width = width;
        this.m_height = height;

        // Determine if we actually need a monitor. In order to do this, simply check if
        // any component monitor been wrapped as a peripheral. Whilst this flag may be
        // out of date,
        boolean needsTerminal = false;
        terminalCheck:
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TileMonitor monitor = this.getNeighbour(x, y);
                if (monitor != null && monitor.peripheral != null) {
                    needsTerminal = true;
                    break terminalCheck;
                }
            }
        }

        // Either delete the current monitor or sync a new one.
        if (needsTerminal) {
            if (this.m_serverMonitor == null) {
                this.m_serverMonitor = new ServerMonitor(this.advanced, this);
            }
        } else {
            this.m_serverMonitor = null;
        }

        // Update the terminal's width and height and rebuild it. This ensures the monitor
        // is consistent when syncing it to other monitors.
        if (this.m_serverMonitor != null) {
            this.m_serverMonitor.rebuild();
        }

        // Update the other monitors, setting coordinates, dimensions and the server terminal
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TileMonitor monitor = this.getNeighbour(x, y);
                if (monitor == null) {
                    continue;
                }

                monitor.m_xIndex = x;
                monitor.m_yIndex = y;
                monitor.m_width = width;
                monitor.m_height = height;
                monitor.m_serverMonitor = this.m_serverMonitor;
                monitor.updateBlockState();
                monitor.updateBlock();
            }
        }
    }

    private boolean mergeLeft() {
        TileMonitor left = this.getNeighbour(-1, 0);
        if (left == null || left.m_yIndex != 0 || left.m_height != this.m_height) {
            return false;
        }

        int width = left.m_width + this.m_width;
        if (width > ComputerCraft.monitorWidth) {
            return false;
        }

        TileMonitor origin = left.getOrigin();
        if (origin != null) {
            origin.resize(width, this.m_height);
        }
        left.expand();
        return true;
    }

    private boolean mergeRight() {
        TileMonitor right = this.getNeighbour(this.m_width, 0);
        if (right == null || right.m_yIndex != 0 || right.m_height != this.m_height) {
            return false;
        }

        int width = this.m_width + right.m_width;
        if (width > ComputerCraft.monitorWidth) {
            return false;
        }

        TileMonitor origin = this.getOrigin();
        if (origin != null) {
            origin.resize(width, this.m_height);
        }
        this.expand();
        return true;
    }

    private boolean mergeUp() {
        TileMonitor above = this.getNeighbour(0, this.m_height);
        if (above == null || above.m_xIndex != 0 || above.m_width != this.m_width) {
            return false;
        }

        int height = above.m_height + this.m_height;
        if (height > ComputerCraft.monitorHeight) {
            return false;
        }

        TileMonitor origin = this.getOrigin();
        if (origin != null) {
            origin.resize(this.m_width, height);
        }
        this.expand();
        return true;
    }

    private boolean mergeDown() {
        TileMonitor below = this.getNeighbour(0, -1);
        if (below == null || below.m_xIndex != 0 || below.m_width != this.m_width) {
            return false;
        }

        int height = this.m_height + below.m_height;
        if (height > ComputerCraft.monitorHeight) {
            return false;
        }

        TileMonitor origin = below.getOrigin();
        if (origin != null) {
            origin.resize(this.m_width, height);
        }
        below.expand();
        return true;
    }

    @SuppressWarnings ("StatementWithEmptyBody")
    void expand() {
        while (this.mergeLeft() || this.mergeRight() || this.mergeUp() || this.mergeDown()) {
        }
    }

    void contractNeighbours() {
        this.visiting = true;
        if (this.m_xIndex > 0) {
            TileMonitor left = this.getNeighbour(this.m_xIndex - 1, this.m_yIndex);
            if (left != null) {
                left.contract();
            }
        }
        if (this.m_xIndex + 1 < this.m_width) {
            TileMonitor right = this.getNeighbour(this.m_xIndex + 1, this.m_yIndex);
            if (right != null) {
                right.contract();
            }
        }
        if (this.m_yIndex > 0) {
            TileMonitor below = this.getNeighbour(this.m_xIndex, this.m_yIndex - 1);
            if (below != null) {
                below.contract();
            }
        }
        if (this.m_yIndex + 1 < this.m_height) {
            TileMonitor above = this.getNeighbour(this.m_xIndex, this.m_yIndex + 1);
            if (above != null) {
                above.contract();
            }
        }
        this.visiting = false;
    }

    void contract() {
        int height = this.m_height;
        int width = this.m_width;

        TileMonitor origin = this.getOrigin();
        if (origin == null) {
            TileMonitor right = width > 1 ? this.getNeighbour(1, 0) : null;
            TileMonitor below = height > 1 ? this.getNeighbour(0, 1) : null;

            if (right != null) {
                right.resize(width - 1, 1);
            }
            if (below != null) {
                below.resize(width, height - 1);
            }
            if (right != null) {
                right.expand();
            }
            if (below != null) {
                below.expand();
            }

            return;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileMonitor monitor = origin.getNeighbour(x, y);
                if (monitor != null) {
                    continue;
                }

                // Decompose
                TileMonitor above = null;
                TileMonitor left = null;
                TileMonitor right = null;
                TileMonitor below = null;

                if (y > 0) {
                    above = origin;
                    above.resize(width, y);
                }
                if (x > 0) {
                    left = origin.getNeighbour(0, y);
                    left.resize(x, 1);
                }
                if (x + 1 < width) {
                    right = origin.getNeighbour(x + 1, y);
                    right.resize(width - (x + 1), 1);
                }
                if (y + 1 < height) {
                    below = origin.getNeighbour(0, y + 1);
                    below.resize(width, height - (y + 1));
                }

                // Re-expand
                if (above != null) {
                    above.expand();
                }
                if (left != null) {
                    left.expand();
                }
                if (right != null) {
                    right.expand();
                }
                if (below != null) {
                    below.expand();
                }
                return;
            }
        }
    }
    // endregion

    private void monitorTouched(float xPos, float yPos, float zPos) {
        XYPair pair = XYPair.of(xPos, yPos, zPos, this.getDirection(), this.getOrientation())
                            .add(this.m_xIndex, this.m_height - this.m_yIndex - 1);

        if (pair.x > this.m_width - RENDER_BORDER || pair.y > this.m_height - RENDER_BORDER || pair.x < RENDER_BORDER || pair.y < RENDER_BORDER) {
            return;
        }

        ServerTerminal serverTerminal = this.getServerMonitor();
        if (serverTerminal == null || !serverTerminal.isColour()) {
            return;
        }

        Terminal originTerminal = serverTerminal.getTerminal();
        if (originTerminal == null) {
            return;
        }

        double xCharWidth = (this.m_width - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getWidth();
        double yCharHeight = (this.m_height - (RENDER_BORDER + RENDER_MARGIN) * 2.0) / originTerminal.getHeight();

        int xCharPos = (int) Math.min(originTerminal.getWidth(), Math.max((pair.x - RENDER_BORDER - RENDER_MARGIN) / xCharWidth + 1.0, 1.0));
        int yCharPos = (int) Math.min(originTerminal.getHeight(), Math.max((pair.y - RENDER_BORDER - RENDER_MARGIN) / yCharHeight + 1.0, 1.0));

        for (int y = 0; y < this.m_height; y++) {
            for (int x = 0; x < this.m_width; x++) {
                TileMonitor monitor = this.getNeighbour(x, y);
                if (monitor == null) {
                    continue;
                }

                for (IComputerAccess computer : monitor.m_computers) {
                    computer.queueEvent("monitor_touch", computer.getAttachmentName(), xCharPos, yCharPos);
                }
            }
        }
    }

    void addComputer(IComputerAccess computer) {
        this.m_computers.add(computer);
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

    void removeComputer(IComputerAccess computer) {
        this.m_computers.remove(computer);
    }
}
