// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;

public class CableBlockEntity extends BlockEntity {
    private final class CableElement extends WiredModemElement {
        @Override
        public Level getLevel() {
            return CableBlockEntity.this.getLevel();
        }

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

    private boolean refreshPeripheral;
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral(PlatformHelper.get().createPeripheralAccess(this, x -> queueRefreshPeripheral()));
    private @Nullable Runnable modemChanged;

    private boolean refreshConnections = false;

    private final WiredModemElement cable = new CableElement();
    private final WiredNode node = cable.getNode();
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);
    private final WiredModemPeripheral modem = new WiredModemPeripheral(
        new ModemState(() -> TickScheduler.schedule(tickToken)), cable, peripheral, this
    ) {
        @Override
        public Vec3 getPosition() {
            var dir = getModemDirection();
            return Vec3.atCenterOf(dir == null ? getBlockPos() : getBlockPos().relative(dir));
        }
    };

    private final ComponentAccess<WiredElement> connectedElements = PlatformHelper.get().createWiredElementAccess(this, x -> scheduleConnectionsChanged());

    public CableBlockEntity(BlockEntityType<? extends CableBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        modem.removed();
        if (level == null || !level.isClientSide) node.remove();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        refreshConnections = refreshPeripheral = true;
        TickScheduler.schedule(tickToken);
    }

    @Override
    @Deprecated
    public void setBlockState(BlockState state) {
        var direction = getModemDirection();
        var hasCable = hasCable();
        super.setBlockState(state);

        // We invalidate both the modem and element if the modem direction or cable are different.
        if (modemChanged != null && (hasCable() != hasCable || getModemDirection() != direction)) modemChanged.run();
    }

    @Nullable
    private Direction getModemDirection() {
        return getBlockState().getValue(CableBlock.MODEM).getFacing();
    }

    void neighborChanged(BlockPos neighbour) {
        var dir = getModemDirection();
        if (!getLevel().isClientSide && dir != null && getBlockPos().relative(dir).equals(neighbour) && isPeripheralOn()) {
            queueRefreshPeripheral();
        }
    }

    void queueRefreshPeripheral() {
        refreshPeripheral = true;
        TickScheduler.schedule(tickToken);
    }

    InteractionResult use(Player player) {
        if (!canAttachPeripheral()) return InteractionResult.FAIL;

        if (getLevel().isClientSide) return InteractionResult.SUCCESS;

        var oldName = peripheral.getConnectedName();
        if (isPeripheralOn()) {
            detachPeripheral();
        } else {
            attachPeripheral();
        }
        var newName = peripheral.getConnectedName();

        if (!Objects.equals(newName, oldName)) {
            if (oldName != null) {
                player.displayClientMessage(Component.translatable("chat.computercraft.wired_modem.peripheral_disconnected",
                    ChatHelpers.copy(oldName)), false);
            }
            if (newName != null) {
                player.displayClientMessage(Component.translatable("chat.computercraft.wired_modem.peripheral_connected",
                    ChatHelpers.copy(newName)), false);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        peripheral.read(nbt, "");
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        peripheral.write(nbt, "");
        super.saveAdditional(nbt);
    }

    private void updateBlockState() {
        var state = getBlockState();
        var oldVariant = state.getValue(CableBlock.MODEM);
        var newVariant = CableModemVariant
            .from(oldVariant.getFacing(), modem.getModemState().isOpen(), peripheral.hasPeripheral());

        if (oldVariant != newVariant) {
            getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(CableBlock.MODEM, newVariant));
        }
    }

    void blockTick() {
        if (getLevel().isClientSide) return;

        if (refreshPeripheral) {
            refreshPeripheral = false;
            if (isPeripheralOn()) attachPeripheral();
        }

        if (modem.getModemState().pollChanged()) updateBlockState();

        if (refreshConnections) connectionsChanged();
    }

    void scheduleConnectionsChanged() {
        refreshConnections = true;
        TickScheduler.schedule(tickToken);
    }

    void connectionsChanged() {
        if (getLevel().isClientSide) return;
        refreshConnections = false;

        var state = getBlockState();
        var world = getLevel();
        var current = getBlockPos();
        for (var facing : DirectionUtil.FACINGS) {
            var offset = current.relative(facing);
            if (!world.isLoaded(offset)) continue;

            var element = connectedElements.get(facing);
            if (element == null) continue;

            var node = element.getNode();
            if (CableBlock.canConnectIn(state, facing)) {
                // If we can connect to it then do so
                this.node.connectTo(node);
            } else {
                // Otherwise break the connection.
                this.node.disconnectFrom(node);
            }
        }

        // If we can no longer attach peripherals, then detach any which may have existed
        if (!canAttachPeripheral()) detachPeripheral();
    }

    private void attachPeripheral() {
        var dir = Objects.requireNonNull(getModemDirection(), "Attaching without a modem");
        if (peripheral.attach(getLevel(), getBlockPos(), dir)) updateConnectedPeripherals();
        updateBlockState();
    }

    private void detachPeripheral() {
        if (peripheral.detach()) updateConnectedPeripherals();
        updateBlockState();
    }

    private void updateConnectedPeripherals() {
        node.updatePeripherals(peripheral.toMap());
    }

    @Nullable
    public WiredElement getWiredElement(@Nullable Direction direction) {
        return direction == null || CableBlock.canConnectIn(getBlockState(), direction) ? cable : null;
    }

    @Nullable
    public IPeripheral getPeripheral(@Nullable Direction direction) {
        return direction == null || getModemDirection() == direction ? modem : null;
    }

    private boolean isPeripheralOn() {
        return getBlockState().getValue(CableBlock.MODEM).isPeripheralOn();
    }

    public void onModemChanged(Runnable callback) {
        modemChanged = callback;
    }

    boolean hasCable() {
        return getBlockState().getValue(CableBlock.CABLE);
    }

    public boolean hasModem() {
        return getBlockState().getValue(CableBlock.MODEM) != CableModemVariant.None;
    }

    private boolean canAttachPeripheral() {
        return hasCable() && hasModem();
    }
}
