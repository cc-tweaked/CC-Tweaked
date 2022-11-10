/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
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
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.inventory.TurtleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;

public class TurtleBlockEntity extends AbstractComputerBlockEntity implements BasicContainer, ITurtleBlockEntity {
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    enum MoveState {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> previousInventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean inventoryChanged = false;
    private TurtleBrain brain = new TurtleBrain(this);
    private MoveState moveState = MoveState.NOT_MOVED;
    private @Nullable IPeripheral peripheral;
    private @Nullable Runnable onMoved;

    public TurtleBlockEntity(BlockEntityType<? extends TurtleBlockEntity> type, BlockPos pos, BlockState state, ComputerFamily family) {
        super(type, pos, state, family);
    }

    private boolean hasMoved() {
        return moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer(int id) {
        var computer = new ServerComputer(
            (ServerLevel) getLevel(), getBlockPos(), id, label,
            getFamily(), Config.turtleTermWidth,
            Config.turtleTermHeight
        );
        computer.addAPI(new TurtleAPI(computer.getAPIEnvironment(), getAccess()));
        brain.setupComputer(computer);
        return computer;
    }

    @Override
    protected void unload() {
        if (!hasMoved()) super.unload();
    }

    @Override
    public InteractionResult use(Player player, InteractionHand hand) {
        // Apply dye
        var currentItem = player.getItemInHand(hand);
        if (!currentItem.isEmpty()) {
            if (currentItem.getItem() instanceof DyeItem dyeItem) {
                // Dye to change turtle colour
                if (!getLevel().isClientSide) {
                    var dye = dyeItem.getDyeColor();
                    if (brain.getDyeColour() != dye) {
                        brain.setDyeColour(dye);
                        if (!player.isCreative()) {
                            currentItem.shrink(1);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (currentItem.getItem() == Items.WATER_BUCKET && brain.getColour() != -1) {
                // Water to remove turtle colour
                if (!getLevel().isClientSide) {
                    if (brain.getColour() != -1) {
                        brain.setColour(-1);
                        if (!player.isCreative()) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                            player.getInventory().setChanged();
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // Open GUI or whatever
        return super.use(player, hand);
    }

    @Override
    protected boolean canNameWithTag(Player player) {
        return true;
    }

    @Override
    protected double getInteractRange() {
        return 12.0;
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        brain.update();
        if (inventoryChanged) {
            var computer = getServerComputer();
            if (computer != null) computer.queueEvent("turtle_inventory");

            inventoryChanged = false;
            for (var n = 0; n < getContainerSize(); n++) {
                previousInventory.set(n, getItem(n).copy());
            }
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
        var nbttaglist = nbt.getList("Items", Tag.TAG_COMPOUND);
        inventory.clear();
        previousInventory.clear();
        for (var i = 0; i < nbttaglist.size(); i++) {
            var tag = nbttaglist.getCompound(i);
            var slot = tag.getByte("Slot") & 0xff;
            if (slot < getContainerSize()) {
                inventory.set(slot, ItemStack.of(tag));
                previousInventory.set(slot, inventory.get(slot).copy());
            }
        }

        // Read state
        brain.readFromNBT(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        // Write inventory
        var nbttaglist = new ListTag();
        for (var i = 0; i < INVENTORY_SIZE; i++) {
            if (!inventory.get(i).isEmpty()) {
                var tag = new CompoundTag();
                tag.putByte("Slot", (byte) i);
                inventory.get(i).save(tag);
                nbttaglist.add(tag);
            }
        }
        nbt.put("Items", nbttaglist);

        // Write brain
        nbt = brain.writeToNBT(nbt);

        super.saveAdditional(nbt);
    }

    @Override
    protected boolean isPeripheralBlockedOnSide(ComputerSide localSide) {
        return hasPeripheralUpgradeOnSide(localSide);
    }

    // IDirectionalTile

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(TurtleBlock.FACING);
    }

    public void setDirection(Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) dir = Direction.NORTH;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(TurtleBlock.FACING, dir));

        updateOutput();
        updateInputsImmediately();

        onTileEntityChange();
    }

    // ITurtleTile

    @Override
    public @Nullable ITurtleUpgrade getUpgrade(TurtleSide side) {
        return brain.getUpgrade(side);
    }

    @Override
    public int getColour() {
        return brain.getColour();
    }

    @Override
    public @Nullable ResourceLocation getOverlay() {
        return brain.getOverlay();
    }

    @Override
    public ITurtleAccess getAccess() {
        return brain;
    }

    @Override
    public Vec3 getRenderOffset(float f) {
        return brain.getRenderOffset(f);
    }

    @Override
    public float getRenderYaw(float f) {
        return brain.getVisualYaw(f);
    }

    @Override
    public float getToolRenderAngle(TurtleSide side, float f) {
        return brain.getToolRenderAngle(side, f);
    }

    void setOwningPlayer(GameProfile player) {
        brain.setOwningPlayer(player);
        setChanged();
    }

    // IInventory

    @Override
    public NonNullList<ItemStack> getContents() {
        return inventory;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!inventoryChanged) {
            for (var n = 0; n < getContainerSize(); n++) {
                if (!ItemStack.matches(getItem(n), previousInventory.get(n))) {
                    inventoryChanged = true;
                    break;
                }
            }
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
        Collections.copy(previousInventory, copy.previousInventory);
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
