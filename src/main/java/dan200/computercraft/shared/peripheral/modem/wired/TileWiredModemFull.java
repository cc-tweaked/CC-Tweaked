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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull.MODEM_ON;
import static dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull.PERIPHERAL_ON;

public class TileWiredModemFull extends TileGeneric {
    private static final String NBT_PERIPHERAL_ENABLED = "PeripheralAccess";

    private static final class FullElement extends WiredModemElement {
        private final TileWiredModemFull entity;

        private FullElement(TileWiredModemFull entity) {
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

        @Nonnull
        @Override
        public Level getLevel() {
            return entity.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition() {
            return Vec3.atCenterOf(entity.getBlockPos());
        }
    }

    private final WiredModemPeripheral[] modems = new WiredModemPeripheral[6];

    private boolean peripheralAccessAllowed = false;
    private final WiredModemLocalPeripheral[] peripherals = new WiredModemLocalPeripheral[6];

    private boolean destroyed = false;
    private boolean connectionsFormed = false;

    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);
    private final ModemState modemState = new ModemState(() -> TickScheduler.schedule(tickToken));
    private final WiredModemElement element = new FullElement(this);
    private final IWiredNode node = element.getNode();

    private final ComponentAccess<IWiredElement> connectedElements = PlatformHelper.get().createWiredElementAccess(x -> connectionsChanged());

    private int invalidSides = 0;

    public TileWiredModemFull(BlockEntityType<TileWiredModemFull> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (var i = 0; i < peripherals.length; i++) {
            var facing = Direction.from3DDataValue(i);
            peripherals[i] = new WiredModemLocalPeripheral(() -> queueRefreshPeripheral(facing));
        }
    }

    private void doRemove() {
        if (level == null || !level.isClientSide) {
            node.remove();
            connectionsFormed = false;
        }
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            destroyed = true;
            doRemove();
        }
        super.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        doRemove();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        doRemove();
    }

    @Override
    public void onNeighbourChange(@Nonnull BlockPos neighbour) {
        onNeighbourTileEntityChange(neighbour);
    }

    @Override
    public void onNeighbourTileEntityChange(@Nonnull BlockPos neighbour) {
        if (!level.isClientSide && peripheralAccessAllowed) {
            for (var facing : DirectionUtil.FACINGS) {
                if (getBlockPos().relative(facing).equals(neighbour)) queueRefreshPeripheral(facing);
            }
        }
    }

    private void queueRefreshPeripheral(@Nonnull Direction facing) {
        if (invalidSides == 0) TickScheduler.schedule(tickToken);
        invalidSides |= 1 << facing.ordinal();
    }

    private void refreshPeripheral(@Nonnull Direction facing) {
        invalidSides &= ~(1 << facing.ordinal());
        var peripheral = peripherals[facing.ordinal()];
        if (level != null && !isRemoved() && peripheral.attach(level, getBlockPos(), facing)) {
            updateConnectedPeripherals();
        }
    }

    @Nonnull
    @Override
    public InteractionResult onActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching() || !player.mayBuild()) return InteractionResult.PASS;
        if (getLevel().isClientSide) return InteractionResult.SUCCESS;

        // On server, we interacted if a peripheral was found
        var oldPeriphNames = getConnectedPeripheralNames();
        togglePeripheralAccess();
        var periphNames = getConnectedPeripheralNames();

        if (!Objects.equal(periphNames, oldPeriphNames)) {
            sendPeripheralChanges(player, "chat.computercraft.wired_modem.peripheral_disconnected", oldPeriphNames);
            sendPeripheralChanges(player, "chat.computercraft.wired_modem.peripheral_connected", periphNames);
        }

        return InteractionResult.SUCCESS;
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
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        peripheralAccessAllowed = nbt.getBoolean(NBT_PERIPHERAL_ENABLED);
        for (var i = 0; i < peripherals.length; i++) peripherals[i].read(nbt, Integer.toString(i));
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        nbt.putBoolean(NBT_PERIPHERAL_ENABLED, peripheralAccessAllowed);
        for (var i = 0; i < peripherals.length; i++) peripherals[i].write(nbt, Integer.toString(i));
        super.saveAdditional(nbt);
    }

    private void updateBlockState() {
        var state = getBlockState();
        boolean modemOn = modemState.isOpen(), peripheralOn = peripheralAccessAllowed;
        if (state.getValue(MODEM_ON) == modemOn && state.getValue(PERIPHERAL_ON) == peripheralOn) return;

        getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(MODEM_ON, modemOn).setValue(PERIPHERAL_ON, peripheralOn));
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // TODO: Replace with onLoad
        TickScheduler.schedule(tickToken);
    }

    @Override
    public void blockTick() {
        if (getLevel().isClientSide) return;

        if (invalidSides != 0) {
            for (var direction : DirectionUtil.FACINGS) {
                if ((invalidSides & (1 << direction.ordinal())) != 0) refreshPeripheral(direction);
            }
        }

        if (modemState.pollChanged()) updateBlockState();

        if (!connectionsFormed) {
            connectionsFormed = true;

            connectionsChanged();
            if (peripheralAccessAllowed) {
                for (var facing : DirectionUtil.FACINGS) {
                    peripherals[facing.ordinal()].attach(level, getBlockPos(), facing);
                }
                updateConnectedPeripherals();
            }
        }
    }

    private void connectionsChanged() {
        if (getLevel().isClientSide) return;

        var world = getLevel();
        var current = getBlockPos();
        for (var facing : DirectionUtil.FACINGS) {
            var offset = current.relative(facing);
            if (!world.isLoaded(offset)) continue;

            var element = connectedElements.get((ServerLevel) getLevel(), getBlockPos(), facing);
            if (element == null) return;

            node.connectTo(element.getNode());
        }
    }

    private void togglePeripheralAccess() {
        if (!peripheralAccessAllowed) {
            var hasAny = false;
            for (var facing : DirectionUtil.FACINGS) {
                var peripheral = peripherals[facing.ordinal()];
                peripheral.attach(level, getBlockPos(), facing);
                hasAny |= peripheral.hasPeripheral();
            }

            if (!hasAny) return;

            peripheralAccessAllowed = true;
            node.updatePeripherals(getConnectedPeripherals());
        } else {
            peripheralAccessAllowed = false;

            for (var peripheral : peripherals) peripheral.detach();
            node.updatePeripherals(Collections.emptyMap());
        }

        updateBlockState();
    }

    private Set<String> getConnectedPeripheralNames() {
        if (!peripheralAccessAllowed) return Collections.emptySet();

        Set<String> peripherals = new HashSet<>(6);
        for (var peripheral : this.peripherals) {
            var name = peripheral.getConnectedName();
            if (name != null) peripherals.add(name);
        }
        return peripherals;
    }

    private Map<String, IPeripheral> getConnectedPeripherals() {
        if (!peripheralAccessAllowed) return Collections.emptyMap();

        Map<String, IPeripheral> peripherals = new HashMap<>(6);
        for (var peripheral : this.peripherals) peripheral.extendMap(peripherals);
        return peripherals;
    }

    private void updateConnectedPeripherals() {
        var peripherals = getConnectedPeripherals();
        if (peripherals.isEmpty()) {
            // If there are no peripherals then disable access and update the display state.
            peripheralAccessAllowed = false;
            updateBlockState();
        }

        node.updatePeripherals(peripherals);
    }

    public IWiredElement getElement() {
        return element;
    }

    @Nullable
    public WiredModemPeripheral getPeripheral(@Nullable Direction side) {
        if (side == null) return null;

        var peripheral = modems[side.ordinal()];
        if (peripheral != null) return peripheral;

        var localPeripheral = peripherals[side.ordinal()];
        return modems[side.ordinal()] = new WiredModemPeripheral(modemState, element) {
            @Nonnull
            @Override
            protected WiredModemLocalPeripheral getLocalPeripheral() {
                return localPeripheral;
            }

            @Nonnull
            @Override
            public Vec3 getPosition() {
                return Vec3.atCenterOf(getBlockPos().relative(side));
            }

            @Nonnull
            @Override
            public Object getTarget() {
                return TileWiredModemFull.this;
            }
        };
    }
}
