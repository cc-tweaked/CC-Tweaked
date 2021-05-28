/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Objects;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TileCable extends TileGeneric implements IPeripheralTile {
    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";
    private final WiredModemLocalPeripheral m_peripheral = new WiredModemLocalPeripheral();
    private final WiredModemElement m_cable = new CableElement();
    private final IWiredNode m_node = this.m_cable.getNode();
    private boolean m_peripheralAccessAllowed;
    private boolean m_destroyed = false;
    private Direction modemDirection = Direction.NORTH;
    private final WiredModemPeripheral m_modem = new WiredModemPeripheral(new ModemState(() -> TickScheduler.schedule(this)), this.m_cable) {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral() {
            return TileCable.this.m_peripheral;
        }

        @Nonnull
        @Override
        public Vec3d getPosition() {
            BlockPos pos = TileCable.this.getPos().offset(TileCable.this.modemDirection);
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Nonnull
        @Override
        public Object getTarget() {
            return TileCable.this;
        }
    };
    private boolean hasModemDirection = false;
    private boolean m_connectionsFormed = false;
    public TileCable(BlockEntityType<? extends TileCable> type) {
        super(type);
    }

    @Override
    public void destroy() {
        if (!this.m_destroyed) {
            this.m_destroyed = true;
            this.m_modem.destroy();
            this.onRemove();
        }
    }

    private void onRemove() {
        if (this.world == null || !this.world.isClient) {
            this.m_node.remove();
            this.m_connectionsFormed = false;
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isInSneakingPose()) {
            return ActionResult.PASS;
        }
        if (!this.canAttachPeripheral()) {
            return ActionResult.FAIL;
        }

        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        String oldName = this.m_peripheral.getConnectedName();
        this.togglePeripheralAccess();
        String newName = this.m_peripheral.getConnectedName();
        if (!Objects.equal(newName, oldName)) {
            if (oldName != null) {
                player.sendMessage(new TranslatableText("chat.computercraft.wired_modem.peripheral_disconnected",
                        ChatHelpers.copy(oldName)), false);
            }
            if (newName != null) {
                player.sendMessage(new TranslatableText("chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy(newName)), false);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onNeighbourChange(@Nonnull BlockPos neighbour) {
        Direction dir = this.getDirection();
        if (neighbour.equals(this.getPos().offset(dir)) && this.hasModem() && !this.getCachedState().canPlaceAt(this.getWorld(), this.getPos())) {
            if (this.hasCable()) {
                // Drop the modem and convert to cable
                Block.dropStack(this.getWorld(), this.getPos(), new ItemStack(ComputerCraftRegistry.ModItems.WIRED_MODEM));
                this.getWorld().setBlockState(this.getPos(),
                                              this.getCachedState().with(BlockCable.MODEM, CableModemVariant.None));
                this.modemChanged();
                this.connectionsChanged();
            } else {
                // Drop everything and remove block
                Block.dropStack(this.getWorld(), this.getPos(), new ItemStack(ComputerCraftRegistry.ModItems.WIRED_MODEM));
                this.getWorld().removeBlock(this.getPos(), false);
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        this.onNeighbourTileEntityChange(neighbour);
    }

    @Nonnull
    private Direction getDirection() {
        this.refreshDirection();
        return this.modemDirection == null ? Direction.NORTH : this.modemDirection;
    }

    public boolean hasModem() {
        return this.getCachedState().get(BlockCable.MODEM) != CableModemVariant.None;
    }

    boolean hasCable() {
        return this.getCachedState().get(BlockCable.CABLE);
    }

    void modemChanged() {
        // Tell anyone who cares that the connection state has changed
        if (this.getWorld().isClient) {
            return;
        }

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if (!this.canAttachPeripheral() && this.m_peripheralAccessAllowed) {
            this.m_peripheralAccessAllowed = false;
            this.m_peripheral.detach();
            this.m_node.updatePeripherals(Collections.emptyMap());
            this.markDirty();
            this.updateBlockState();
        }
    }

    void connectionsChanged() {
        if (this.getWorld().isClient) {
            return;
        }

        BlockState state = this.getCachedState();
        World world = this.getWorld();
        BlockPos current = this.getPos();
        for (Direction facing : DirectionUtil.FACINGS) {
            BlockPos offset = current.offset(facing);
            if (!world.isChunkLoaded(offset)) {
                continue;
            }

            IWiredElement element = ComputerCraftAPI.getWiredElementAt(world, offset, facing.getOpposite());
            if (element != null) {
                // TODO Figure out why this crashes.
                IWiredNode node = element.getNode();
                if (node != null && this.m_node != null) {
                    if (BlockCable.canConnectIn(state, facing)) {
                        // If we can connect to it then do so
                        this.m_node.connectTo(node);
                    } else if (this.m_node.getNetwork() == node.getNetwork()) {
                        // Otherwise if we're on the same network then attempt to void it.
                        this.m_node.disconnectFrom(node);
                    }
                }
            }
        }
    }

    private boolean canAttachPeripheral() {
        return this.hasCable() && this.hasModem();
    }

    private void updateBlockState() {
        BlockState state = this.getCachedState();
        CableModemVariant oldVariant = state.get(BlockCable.MODEM);
        CableModemVariant newVariant = CableModemVariant.from(oldVariant.getFacing(), this.m_modem.getModemState()
                                                                                                  .isOpen(), this.m_peripheralAccessAllowed);

        if (oldVariant != newVariant) {
            this.world.setBlockState(this.getPos(), state.with(BlockCable.MODEM, newVariant));
        }
    }

    private void refreshPeripheral() {
        if (this.world != null && !this.isRemoved() && this.m_peripheral.attach(this.world, this.getPos(), this.getDirection())) {
            this.updateConnectedPeripherals();
        }
    }

    private void updateConnectedPeripherals() {
        Map<String, IPeripheral> peripherals = this.m_peripheral.toMap();
        if (peripherals.isEmpty()) {
            // If there are no peripherals then disable access and update the display state.
            this.m_peripheralAccessAllowed = false;
            this.updateBlockState();
        }

        this.m_node.updatePeripherals(peripherals);
    }

    @Override
    public void onNeighbourTileEntityChange(@Nonnull BlockPos neighbour) {
        super.onNeighbourTileEntityChange(neighbour);
        if (!this.world.isClient && this.m_peripheralAccessAllowed) {
            Direction facing = this.getDirection();
            if (this.getPos().offset(facing)
                    .equals(neighbour)) {
                this.refreshPeripheral();
            }
        }
    }

    @Override
    public void blockTick() {
        if (this.getWorld().isClient) {
            return;
        }

        this.refreshDirection();

        if (this.m_modem.getModemState()
                        .pollChanged()) {
            this.updateBlockState();
        }

        if (!this.m_connectionsFormed) {
            this.m_connectionsFormed = true;

            this.connectionsChanged();
            if (this.m_peripheralAccessAllowed) {
                this.m_peripheral.attach(this.world, this.pos, this.modemDirection);
                this.updateConnectedPeripherals();
            }
        }
    }

    private void togglePeripheralAccess() {
        if (!this.m_peripheralAccessAllowed) {
            this.m_peripheral.attach(this.world, this.getPos(), this.getDirection());
            if (!this.m_peripheral.hasPeripheral()) {
                return;
            }

            this.m_peripheralAccessAllowed = true;
            this.m_node.updatePeripherals(this.m_peripheral.toMap());
        } else {
            this.m_peripheral.detach();

            this.m_peripheralAccessAllowed = false;
            this.m_node.updatePeripherals(Collections.emptyMap());
        }

        this.updateBlockState();
    }

    @Nullable
    private Direction getMaybeDirection() {
        this.refreshDirection();
        return this.modemDirection;
    }

    private void refreshDirection() {
        if (this.hasModemDirection) {
            return;
        }

        this.hasModemDirection = true;
        this.modemDirection = this.getCachedState().get(BlockCable.MODEM)
                                  .getFacing();
    }

    @Override
    public void fromTag(@Nonnull BlockState state, @Nonnull CompoundTag nbt) {
        super.fromTag(state, nbt);
        this.m_peripheralAccessAllowed = nbt.getBoolean(NBT_PERIPHERAL_ENABLED);
        this.m_peripheral.read(nbt, "");
    }

    @Nonnull
    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        nbt.putBoolean(NBT_PERIPHERAL_ENABLED, this.m_peripheralAccessAllowed);
        this.m_peripheral.write(nbt, "");
        return super.toTag(nbt);
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        this.onRemove();
    }

    @Override
    public void cancelRemoval() {
        super.cancelRemoval();
        TickScheduler.schedule(this);
    }

    @Override
    public void resetBlock() {
        super.resetBlock();
        this.hasModemDirection = false;
        if (!this.world.isClient) {
            this.world.getBlockTickScheduler()
                      .schedule(this.pos,
                           this.getCachedState().getBlock(), 0);
        }
    }

    public IWiredElement getElement(Direction facing) {
        return BlockCable.canConnectIn(this.getCachedState(), facing) ? this.m_cable : null;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral(Direction side) {
        return !this.m_destroyed && this.hasModem() && side == this.getDirection() ? this.m_modem : null;
    }

    private class CableElement extends WiredModemElement {
        @Nonnull
        @Override
        public World getWorld() {
            return TileCable.this.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition() {
            BlockPos pos = TileCable.this.getPos();
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Override
        protected void attachPeripheral(String name, IPeripheral peripheral) {
            TileCable.this.m_modem.attachPeripheral(name, peripheral);
        }

        @Override
        protected void detachPeripheral(String name) {
            TileCable.this.m_modem.detachPeripheral(name);
        }
    }
}
