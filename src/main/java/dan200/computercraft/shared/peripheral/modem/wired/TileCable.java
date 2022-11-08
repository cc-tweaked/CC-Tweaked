/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

public class TileCable extends TileGeneric {
    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";

    private class CableElement extends WiredModemElement {
        @Nonnull
        @Override
        public Level getLevel() {
            return TileCable.this.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(getBlockPos());
        }

        @Override
        protected void attachPeripheral(String name, IPeripheral peripheral) {
            modem.attachPeripheral(name, peripheral);
        }

        @Override
        protected void detachPeripheral(String name) {
            modem.detachPeripheral(name);
        }
    }

    private boolean invalidPeripheral;
    private boolean peripheralAccessAllowed;
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral(this::queueRefreshPeripheral);
    private @Nullable Runnable modemChanged;

    private boolean destroyed = false;

    private boolean connectionsFormed = false;

    private final WiredModemElement cable = new CableElement();
    private final IWiredNode node = cable.getNode();
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);
    private final WiredModemPeripheral modem = new WiredModemPeripheral(
        new ModemState(() -> TickScheduler.schedule(tickToken)),
        cable
    ) {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral() {
            return peripheral;
        }

        @Nonnull
        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(getBlockPos().relative(getDirection()));
        }

        @Nonnull
        @Override
        public Object getTarget() {
            return TileCable.this;
        }
    };

    private final ComponentAccess<IWiredElement> connectedElements = PlatformHelper.get().createWiredElementAccess(x -> connectionsChanged());

    public TileCable(BlockEntityType<? extends TileCable> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private void onRemove() {
        if (level == null || !level.isClientSide) {
            node.remove();
            connectionsFormed = false;
        }
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            destroyed = true;
            modem.destroy();
            onRemove();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        onRemove();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        onRemove();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // TODO: Replace with onLoad
        TickScheduler.schedule(tickToken);
    }

    @Override
    @Deprecated
    public void setBlockState(@Nonnull BlockState state) {
        var direction = getMaybeDirection();
        super.setBlockState(state);

        // We invalidate both the modem and element if the modem's direction is different.
        if (getMaybeDirection() != direction && modemChanged != null) modemChanged.run();
    }

    @Nullable
    private Direction getMaybeDirection() {
        return getBlockState().getValue(BlockCable.MODEM).getFacing();
    }

    @Nonnull
    private Direction getDirection() {
        var direction = getMaybeDirection();
        return direction == null ? Direction.NORTH : direction;
    }

    @Override
    public void onNeighbourChange(@Nonnull BlockPos neighbour) {
        var dir = getDirection();
        if (neighbour.equals(getBlockPos().relative(dir)) && hasModem() && !getBlockState().canSurvive(getLevel(), getBlockPos())) {
            if (hasCable()) {
                // Drop the modem and convert to cable
                Block.popResource(getLevel(), getBlockPos(), new ItemStack(ModRegistry.Items.WIRED_MODEM.get()));
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(BlockCable.MODEM, CableModemVariant.None));
                modemChanged();
                connectionsChanged();
            } else {
                // Drop everything and remove block
                Block.popResource(getLevel(), getBlockPos(), new ItemStack(ModRegistry.Items.WIRED_MODEM.get()));
                getLevel().removeBlock(getBlockPos(), false);
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        onNeighbourTileEntityChange(neighbour);
    }

    @Override
    public void onNeighbourTileEntityChange(@Nonnull BlockPos neighbour) {
        super.onNeighbourTileEntityChange(neighbour);
        if (!level.isClientSide && peripheralAccessAllowed) {
            var facing = getDirection();
            if (getBlockPos().relative(facing).equals(neighbour)) queueRefreshPeripheral();
        }
    }

    private void queueRefreshPeripheral() {
        if (invalidPeripheral) return;
        invalidPeripheral = true;
        TickScheduler.schedule(tickToken);
    }

    private void refreshPeripheral() {
        invalidPeripheral = false;
        if (level != null && !isRemoved() && peripheral.attach(level, getBlockPos(), getDirection())) {
            updateConnectedPeripherals();
        }
    }

    @Nonnull
    @Override
    public InteractionResult onActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching() || !player.mayBuild()) return InteractionResult.PASS;
        if (!canAttachPeripheral()) return InteractionResult.FAIL;

        if (getLevel().isClientSide) return InteractionResult.SUCCESS;

        var oldName = peripheral.getConnectedName();
        togglePeripheralAccess();
        var newName = peripheral.getConnectedName();
        if (!Objects.equal(newName, oldName)) {
            if (oldName != null) {
                player.displayClientMessage(Component.translatable("chat.computercraft.wired_modem.peripheral_disconnected",
                    ChatHelpers.copy(oldName)), false);
            }
            if (newName != null) {
                player.displayClientMessage(Component.translatable("chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy(newName)), false);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        peripheralAccessAllowed = nbt.getBoolean(NBT_PERIPHERAL_ENABLED);
        peripheral.read(nbt, "");
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        nbt.putBoolean(NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed);
        peripheral.write(nbt, "");
        super.saveAdditional(nbt);
    }

    private void updateBlockState() {
        var state = getBlockState();
        var oldVariant = state.getValue(BlockCable.MODEM);
        var newVariant = CableModemVariant
            .from(oldVariant.getFacing(), modem.getModemState().isOpen(), peripheralAccessAllowed);

        if (oldVariant != newVariant) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(BlockCable.MODEM, newVariant));
        }
    }

    @Override
    public void blockTick() {
        if (getLevel().isClientSide) return;

        if (invalidPeripheral) refreshPeripheral();

        if (modem.getModemState().pollChanged()) updateBlockState();

        if (!connectionsFormed) {
            connectionsFormed = true;

            connectionsChanged();
            if (peripheralAccessAllowed) {
                peripheral.attach(level, worldPosition, getDirection());
                updateConnectedPeripherals();
            }
        }
    }

    void connectionsChanged() {
        if (getLevel().isClientSide) return;

        var state = getBlockState();
        var world = getLevel();
        var current = getBlockPos();
        for (var facing : DirectionUtil.FACINGS) {
            var offset = current.relative(facing);
            if (!world.isLoaded(offset)) continue;

            var element = connectedElements.get((ServerLevel) world, current, facing);
            if (element == null) continue;

            var node = element.getNode();
            if (BlockCable.canConnectIn(state, facing)) {
                // If we can connect to it then do so
                this.node.connectTo(node);
            } else if (this.node.getNetwork() == node.getNetwork()) {
                // Otherwise if we're on the same network then attempt to void it.
                this.node.disconnectFrom(node);
            }
        }
    }

    void modemChanged() {
        // Tell anyone who cares that the connection state has changed
        if (modemChanged != null) modemChanged.run();

        if (getLevel().isClientSide) return;

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if (!canAttachPeripheral() && peripheralAccessAllowed) {
            peripheralAccessAllowed = false;
            peripheral.detach();
            node.updatePeripherals(Collections.emptyMap());
            setChanged();
            updateBlockState();
        }
    }

    private void togglePeripheralAccess() {
        if (!peripheralAccessAllowed) {
            peripheral.attach(level, getBlockPos(), getDirection());
            if (!peripheral.hasPeripheral()) return;

            peripheralAccessAllowed = true;
            node.updatePeripherals(peripheral.toMap());
        } else {
            peripheral.detach();

            peripheralAccessAllowed = false;
            node.updatePeripherals(Collections.emptyMap());
        }

        updateBlockState();
    }

    private void updateConnectedPeripherals() {
        var peripherals = peripheral.toMap();
        if (peripherals.isEmpty()) {
            // If there are no peripherals then disable access and update the display state.
            peripheralAccessAllowed = false;
            updateBlockState();
        }

        node.updatePeripherals(peripherals);
    }

    @Nullable
    public IWiredElement getWiredElement(@Nullable Direction direction) {
        if (destroyed) return null;
        return direction == null || BlockCable.canConnectIn(getBlockState(), direction) ? cable : null;
    }

    @Nullable
    public IPeripheral getPeripheral(@Nullable Direction direction) {
        if (destroyed) return null;
        return direction == null || getMaybeDirection() == direction ? modem : null;
    }

    public void onModemChanged(Runnable callback) {
        modemChanged = callback;
    }

    boolean hasCable() {
        return getBlockState().getValue(BlockCable.CABLE);
    }

    public boolean hasModem() {
        return getBlockState().getValue(BlockCable.MODEM) != CableModemVariant.None;
    }

    private boolean canAttachPeripheral() {
        return hasCable() && hasModem();
    }
}
