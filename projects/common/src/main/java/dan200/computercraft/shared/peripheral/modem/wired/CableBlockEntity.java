// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.ModRegistry;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class CableBlockEntity extends BlockEntity {
    private static final String NBT_PERIPHERAL_ENABLED = "PeripheralAccess";

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

    private boolean invalidPeripheral;
    private boolean peripheralAccessAllowed;
    private final WiredModemLocalPeripheral peripheral = new WiredModemLocalPeripheral(this::queueRefreshPeripheral);
    private @Nullable Runnable modemChanged;

    private boolean connectionsFormed = false;

    private final WiredModemElement cable = new CableElement();
    private final WiredNode node = cable.getNode();
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);
    private final WiredModemPeripheral modem = new WiredModemPeripheral(
        new ModemState(() -> TickScheduler.schedule(tickToken)),
        cable
    ) {
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral() {
            return peripheral;
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(getBlockPos().relative(getDirection()));
        }

        @Override
        public Object getTarget() {
            return CableBlockEntity.this;
        }
    };

    private final ComponentAccess<WiredElement> connectedElements = PlatformHelper.get().createWiredElementAccess(x -> connectionsChanged());

    public CableBlockEntity(BlockEntityType<? extends CableBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private void onRemove() {
        if (level == null || !level.isClientSide) {
            node.remove();
            connectionsFormed = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        modem.removed();
        onRemove();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        TickScheduler.schedule(tickToken);
    }

    @Override
    @Deprecated
    public void setBlockState(BlockState state) {
        var direction = getMaybeDirection();
        super.setBlockState(state);

        // We invalidate both the modem and element if the modem's direction is different.
        if (getMaybeDirection() != direction && modemChanged != null) modemChanged.run();
    }

    @Nullable
    private Direction getMaybeDirection() {
        return getBlockState().getValue(CableBlock.MODEM).getFacing();
    }

    private Direction getDirection() {
        var direction = getMaybeDirection();
        return direction == null ? Direction.NORTH : direction;
    }

    void neighborChanged(BlockPos neighbour) {
        var dir = getDirection();
        if (neighbour.equals(getBlockPos().relative(dir)) && hasModem() && !getBlockState().canSurvive(getLevel(), getBlockPos())) {
            if (hasCable()) {
                // Drop the modem and convert to cable
                Block.popResource(getLevel(), getBlockPos(), new ItemStack(ModRegistry.Items.WIRED_MODEM.get()));
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(CableBlock.MODEM, CableModemVariant.None));
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

    InteractionResult use(Player player) {
        if (player.isCrouching() || !player.mayBuild()) return InteractionResult.PASS;
        if (!canAttachPeripheral()) return InteractionResult.FAIL;

        if (getLevel().isClientSide) return InteractionResult.SUCCESS;

        var oldName = peripheral.getConnectedName();
        togglePeripheralAccess();
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
        // Fallback to the previous (incorrect) key
        peripheralAccessAllowed = nbt.getBoolean(NBT_PERIPHERAL_ENABLED) || nbt.getBoolean("PeirpheralAccess");
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
        var oldVariant = state.getValue(CableBlock.MODEM);
        var newVariant = CableModemVariant
            .from(oldVariant.getFacing(), modem.getModemState().isOpen(), peripheralAccessAllowed);

        if (oldVariant != newVariant) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(CableBlock.MODEM, newVariant));
        }
    }

    void blockTick() {
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
            if (CableBlock.canConnectIn(state, facing)) {
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
            node.updatePeripherals(Map.of());
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
            node.updatePeripherals(Map.of());
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
    public WiredElement getWiredElement(@Nullable Direction direction) {
        return direction == null || CableBlock.canConnectIn(getBlockState(), direction) ? cable : null;
    }

    @Nullable
    public IPeripheral getPeripheral(@Nullable Direction direction) {
        return direction == null || getMaybeDirection() == direction ? modem : null;
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
