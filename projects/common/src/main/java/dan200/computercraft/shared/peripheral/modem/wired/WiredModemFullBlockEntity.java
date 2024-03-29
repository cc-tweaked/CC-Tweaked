// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
import java.util.*;

import static dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlock.MODEM_ON;
import static dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlock.PERIPHERAL_ON;

public class WiredModemFullBlockEntity extends BlockEntity {
    private static final class FullElement extends WiredModemElement {
        private final WiredModemFullBlockEntity entity;

        private FullElement(WiredModemFullBlockEntity entity) {
            this.entity = entity;
        }

        @Override
        protected void attachPeripheral(String name, IPeripheral peripheral) {
            for (var i = 0; i < 6; i++) {
                var modem = entity.modems[i];
                if (modem != null) modem.attachPeripheral(name, peripheral);
            }
        }

        @Override
        protected void detachPeripheral(String name) {
            for (var i = 0; i < 6; i++) {
                var modem = entity.modems[i];
                if (modem != null) modem.detachPeripheral(name);
            }
        }

        @Override
        public Level getLevel() {
            return entity.getLevel();
        }

        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(entity.getBlockPos());
        }
    }

    private final WiredModemPeripheral[] modems = new WiredModemPeripheral[6];

    private final WiredModemLocalPeripheral[] peripherals = new WiredModemLocalPeripheral[6];

    private boolean refreshConnections = false;

    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);
    private final ModemState modemState = new ModemState(() -> TickScheduler.schedule(tickToken));
    private final WiredModemElement element = new FullElement(this);
    private final WiredNode node = element.getNode();

    private final ComponentAccess<WiredElement> connectedElements = PlatformHelper.get().createWiredElementAccess(this, x -> scheduleConnectionsChanged());

    private int invalidSides = 0;

    public WiredModemFullBlockEntity(BlockEntityType<WiredModemFullBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var peripheralAccess = PlatformHelper.get().createPeripheralAccess(this, this::queueRefreshPeripheral);
        for (var i = 0; i < peripherals.length; i++) {
            peripherals[i] = new WiredModemLocalPeripheral(peripheralAccess);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        for (var modem : modems) {
            if (modem != null) modem.removed();
        }
        if (level == null || !level.isClientSide) node.remove();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        refreshConnections = true;
        invalidSides = DirectionUtil.ALL_SIDES;
        TickScheduler.schedule(tickToken);
    }

    void neighborChanged(BlockPos neighbour) {
        for (var facing : DirectionUtil.FACINGS) {
            if (getBlockPos().relative(facing).equals(neighbour)) queueRefreshPeripheral(facing);
        }
    }

    void queueRefreshPeripheral(Direction facing) {
        invalidSides |= 1 << facing.ordinal();
        TickScheduler.schedule(tickToken);
    }

    public InteractionResult use(Player player) {
        if (player.isCrouching() || !player.mayBuild()) return InteractionResult.PASS;
        if (getLevel().isClientSide) return InteractionResult.SUCCESS;

        // On server, we interacted if a peripheral was found
        var oldPeriphNames = getConnectedPeripheralNames();
        if (isPeripheralOn()) {
            detachPeripherals();
        } else {
            attachPeripherals(DirectionUtil.ALL_SIDES);
        }
        var periphNames = getConnectedPeripheralNames();

        if (!Objects.equals(periphNames, oldPeriphNames)) {
            sendPeripheralChanges(player, "chat.computercraft.wired_modem.peripheral_disconnected", oldPeriphNames);
            sendPeripheralChanges(player, "chat.computercraft.wired_modem.peripheral_connected", periphNames);
        }

        return InteractionResult.CONSUME;
    }

    private static void sendPeripheralChanges(Player player, String kind, Collection<String> peripherals) {
        if (peripherals.isEmpty()) return;

        List<String> names = new ArrayList<>(peripherals);
        names.sort(Comparator.naturalOrder());

        var base = Component.literal("");
        for (var i = 0; i < names.size(); i++) {
            if (i > 0) base.append(", ");
            base.append(ChatHelpers.copy(names.get(i)));
        }

        player.displayClientMessage(Component.translatable(kind, base), false);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        for (var i = 0; i < peripherals.length; i++) peripherals[i].read(nbt, Integer.toString(i));
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        for (var i = 0; i < peripherals.length; i++) peripherals[i].write(nbt, Integer.toString(i));
        super.saveAdditional(nbt);
    }

    void blockTick() {
        if (getLevel().isClientSide) return;

        if (invalidSides != 0) {
            var oldInvalidSides = invalidSides;
            invalidSides = 0;
            if (isPeripheralOn()) attachPeripherals(oldInvalidSides);
        }

        if (modemState.pollChanged()) updateModemBlockState();

        if (refreshConnections) connectionsChanged();
    }

    private void updateModemBlockState() {
        var state = getBlockState();
        var modemOn = modemState.isOpen();
        if (state.getValue(MODEM_ON) == modemOn) return;

        getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(MODEM_ON, modemOn));
    }

    private void scheduleConnectionsChanged() {
        refreshConnections = true;
        TickScheduler.schedule(tickToken);
    }

    private void connectionsChanged() {
        if (getLevel().isClientSide) return;
        refreshConnections = false;

        var world = getLevel();
        var current = getBlockPos();
        for (var facing : DirectionUtil.FACINGS) {
            var offset = current.relative(facing);
            if (!world.isLoaded(offset)) continue;

            var element = connectedElements.get(facing);
            if (element == null) continue;

            node.connectTo(element.getNode());
        }
    }

    private List<String> getConnectedPeripheralNames() {
        List<String> peripherals = new ArrayList<>(6);
        for (var peripheral : this.peripherals) {
            var name = peripheral.getConnectedName();
            if (name != null) peripherals.add(name);
        }
        peripherals.sort(String::compareTo);
        return peripherals;
    }

    private void attachPeripherals(int sides) {
        var anyChanged = false;

        Map<String, IPeripheral> attachedPeripherals = new HashMap<>(6);

        for (var facing : DirectionUtil.FACINGS) {
            var peripheral = peripherals[facing.ordinal()];
            if (DirectionUtil.isSet(sides, facing)) anyChanged |= peripheral.attach(getLevel(), getBlockPos(), facing);
            peripheral.extendMap(attachedPeripherals);
        }

        if (anyChanged) node.updatePeripherals(attachedPeripherals);

        updatePeripheralBlocKState(!attachedPeripherals.isEmpty());
    }

    private void detachPeripherals() {
        var anyChanged = false;
        for (var peripheral : peripherals) anyChanged |= peripheral.detach();
        if (anyChanged) node.updatePeripherals(Map.of());

        updatePeripheralBlocKState(false);
    }

    private void updatePeripheralBlocKState(boolean peripheralOn) {
        var state = getBlockState();
        if (state.getValue(PERIPHERAL_ON) == peripheralOn) return;
        getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(PERIPHERAL_ON, peripheralOn));
    }

    private boolean isPeripheralOn() {
        return getBlockState().getValue(PERIPHERAL_ON);
    }

    public WiredElement getElement() {
        return element;
    }

    @Nullable
    public WiredModemPeripheral getPeripheral(@Nullable Direction side) {
        if (side == null) return null;

        var peripheral = modems[side.ordinal()];
        if (peripheral != null) return peripheral;

        return modems[side.ordinal()] = new WiredModemPeripheral(modemState, element, peripherals[side.ordinal()], this) {
            @Override
            public Vec3 getPosition() {
                return Vec3.atCenterOf(getBlockPos().relative(side));
            }
        };
    }
}
