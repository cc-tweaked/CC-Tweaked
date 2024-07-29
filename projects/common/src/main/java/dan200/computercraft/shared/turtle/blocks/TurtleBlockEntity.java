// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.component.ComputerComponents;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.container.BasicContainer;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.inventory.TurtleMenu;
import dan200.computercraft.shared.util.ComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.function.IntSupplier;

public class TurtleBlockEntity extends AbstractComputerBlockEntity implements BasicContainer {
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    enum MoveState {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> inventorySnapshot = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean inventoryChanged = false;

    private final IntSupplier fuelLimit;

    private TurtleBrain brain = new TurtleBrain(this);
    private MoveState moveState = MoveState.NOT_MOVED;
    private @Nullable IPeripheral peripheral;
    private @Nullable Runnable onMoved;

    public TurtleBlockEntity(BlockEntityType<? extends TurtleBlockEntity> type, BlockPos pos, BlockState state, IntSupplier fuelLimit, ComputerFamily family) {
        super(type, pos, state, family);
        this.fuelLimit = fuelLimit;
    }

    boolean hasMoved() {
        return moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer(int id) {
        var computer = new ServerComputer(
            (ServerLevel) getLevel(), getBlockPos(), id, label,
            getFamily(), Config.turtleTermWidth, Config.turtleTermHeight,
            ComponentMap.builder().add(ComputerComponents.TURTLE, brain).build()
        );
        brain.setupComputer(computer);
        return computer;
    }

    @Override
    protected void unload() {
        if (!hasMoved()) super.unload();
    }

    @Override
    protected int getInteractRange() {
        return Container.DEFAULT_DISTANCE_LIMIT + 4;
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        brain.update();
        if (inventoryChanged) {
            var computer = getServerComputer();
            if (computer != null) computer.queueEvent("turtle_inventory");
            inventoryChanged = false;
        }
    }

    protected void clientTick() {
        brain.update();
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
    }

    @Override
    public void neighborChanged(BlockPos neighbour) {
        if (moveState == MoveState.NOT_MOVED) super.neighborChanged(neighbour);
    }

    public void notifyMoveStart() {
        if (moveState == MoveState.NOT_MOVED) moveState = MoveState.IN_PROGRESS;
    }

    public void notifyMoveEnd() {
        // MoveState.MOVED is final
        if (moveState == MoveState.IN_PROGRESS) moveState = MoveState.NOT_MOVED;
    }

    @Override
    public void loadServer(CompoundTag nbt) {
        super.loadServer(nbt);

        // Read inventory
        ContainerHelper.loadAllItems(nbt, inventory);
        for (var i = 0; i < inventory.size(); i++) inventorySnapshot.set(i, inventory.get(i).copy());

        // Read state
        brain.readFromNBT(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        // Write inventory
        ContainerHelper.saveAllItems(nbt, inventory);

        // Write brain
        nbt = brain.writeToNBT(nbt);

        super.saveAdditional(nbt);
    }

    @Override
    protected boolean isPeripheralBlockedOnSide(ComputerSide localSide) {
        return hasPeripheralUpgradeOnSide(localSide);
    }

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(TurtleBlock.FACING);
    }

    public void setDirection(Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) dir = Direction.NORTH;
        getLevel().setBlockAndUpdate(worldPosition, getBlockState().setValue(TurtleBlock.FACING, dir));

        updateRedstone();
        updateInputsImmediately();

        onTileEntityChange();
    }

    public @Nullable ITurtleUpgrade getUpgrade(TurtleSide side) {
        return brain.getUpgrade(side);
    }

    public int getColour() {
        return brain.getColour();
    }

    public @Nullable ResourceLocation getOverlay() {
        return brain.getOverlay();
    }

    public ITurtleAccess getAccess() {
        return brain;
    }

    public Vec3 getRenderOffset(float f) {
        return brain.getRenderOffset(f);
    }

    public float getRenderYaw(float f) {
        return brain.getVisualYaw(f);
    }

    public float getToolRenderAngle(TurtleSide side, float f) {
        return brain.getToolRenderAngle(side, f);
    }

    void setOwningPlayer(GameProfile player) {
        brain.setOwningPlayer(player);
        onTileEntityChange();
    }

    // IInventory

    @Override
    public NonNullList<ItemStack> getContents() {
        return inventory;
    }

    public ItemStack getItemSnapshot(int slot) {
        return slot >= 0 && slot < inventorySnapshot.size() ? inventorySnapshot.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setChanged() {
        super.setChanged();

        for (var slot = 0; slot < getContainerSize(); slot++) {
            var item = getItem(slot);
            if (ItemStack.matches(item, inventorySnapshot.get(slot))) continue;

            inventoryChanged = true;
            inventorySnapshot.set(slot, item.copy());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return isUsable(player);
    }

    public void onTileEntityChange() {
        super.setChanged();
    }

    // Networking stuff

    @Override
    public CompoundTag getUpdateTag() {
        var nbt = super.getUpdateTag();
        brain.writeDescription(nbt);
        return nbt;
    }

    @Override
    public void loadClient(CompoundTag nbt) {
        super.loadClient(nbt);
        brain.readDescription(nbt);
    }

    // Privates

    public int getFuelLimit() {
        return fuelLimit.getAsInt();
    }

    private boolean hasPeripheralUpgradeOnSide(ComputerSide side) {
        ITurtleUpgrade upgrade;
        switch (side) {
            case RIGHT:
                upgrade = getUpgrade(TurtleSide.RIGHT);
                break;
            case LEFT:
                upgrade = getUpgrade(TurtleSide.LEFT);
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom(TurtleBlockEntity copy) {
        super.transferStateFrom(copy);
        Collections.copy(inventory, copy.inventory);
        Collections.copy(inventorySnapshot, copy.inventorySnapshot);
        inventoryChanged = copy.inventoryChanged;
        brain = copy.brain;
        brain.setOwner(this);

        // Mark the other turtle as having moved, and so its peripheral is dead.
        copy.moveState = MoveState.MOVED;
        if (onMoved != null) onMoved.run();
    }

    @Nullable
    public IPeripheral peripheral() {
        if (hasMoved()) return null;
        if (peripheral != null) return peripheral;
        return peripheral = new ComputerPeripheral("turtle", this);
    }

    public void onMoved(Runnable onMoved) {
        this.onMoved = onMoved;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return TurtleMenu.ofBrain(id, inventory, brain);
    }
}
