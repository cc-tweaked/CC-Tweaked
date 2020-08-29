/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.util.DefaultInventory;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.NBTUtil;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.RedstoneUtil;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TileTurtle extends TileComputerBase implements ITurtleTile, DefaultInventory, Nameable {
    // Statics

    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    public static final NamedBlockEntityType<TileTurtle> FACTORY_NORMAL = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID, "turtle_normal"),
                                                                                                      type -> new TileTurtle(type, ComputerFamily.Normal));

    public static final NamedBlockEntityType<TileTurtle> FACTORY_ADVANCED = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID,
                                                                                                                       "turtle_advanced"),
                                                                                                        type -> new TileTurtle(type,
                                                                                                                               ComputerFamily.Advanced));

    // Members
    private DefaultedList<ItemStack> m_inventory;
    private DefaultedList<ItemStack> m_previousInventory;
    private boolean m_inventoryChanged;
    private TurtleBrain m_brain;
    private MoveState m_moveState;
    public TileTurtle(BlockEntityType<? extends TileGeneric> type, ComputerFamily family) {
        super(type, family);
        this.m_inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        this.m_previousInventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        this.m_inventoryChanged = false;
        this.m_brain = new TurtleBrain(this);
        this.m_moveState = MoveState.NOT_MOVED;
    }

    @Override
    protected void unload() {
        if (!this.hasMoved()) {
            super.unload();
        }
    }

    @Override
    public void destroy() {
        if (!this.hasMoved()) {
            // Stop computer
            super.destroy();

            // Drop contents
            if (!this.getWorld().isClient) {
                int size = this.size();
                for (int i = 0; i < size; i++) {
                    ItemStack stack = this.getStack(i);
                    if (!stack.isEmpty()) {
                        WorldUtil.dropItemStack(stack, this.getWorld(), this.getPos());
                    }
                }
            }
        } else {
            // Just turn off any redstone we had on
            for (Direction dir : DirectionUtil.FACINGS) {
                RedstoneUtil.propagateRedstoneOutput(this.getWorld(), this.getPos(), dir);
            }
        }
    }

    public boolean hasMoved() {
        return this.m_moveState == MoveState.MOVED;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.m_inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            synchronized (this.m_inventory) {
                return this.m_inventory.get(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int slot, int count) {
        if (count == 0) {
            return ItemStack.EMPTY;
        }

        synchronized (this.m_inventory) {
            ItemStack stack = this.getStack(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (stack.getCount() <= count) {
                this.setStack(slot, ItemStack.EMPTY);
                return stack;
            }

            ItemStack part = stack.split(count);
            this.onInventoryDefinitelyChanged();
            return part;
        }
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int slot) {
        synchronized (this.m_inventory) {
            ItemStack result = this.getStack(slot);
            this.setStack(slot, ItemStack.EMPTY);
            return result;
        }
    }

    @Override
    public void setStack(int i, @Nonnull ItemStack stack) {
        if (i >= 0 && i < INVENTORY_SIZE) {
            synchronized (this.m_inventory) {
                if (!InventoryUtil.areItemsEqual(stack, this.m_inventory.get(i))) {
                    this.m_inventory.set(i, stack);
                    this.onInventoryDefinitelyChanged();
                }
            }
        }
    }

    @Override
    public boolean canPlayerUse(@Nonnull PlayerEntity player) {
        return this.isUsable(player, false);
    }

    private void onInventoryDefinitelyChanged() {
        super.markDirty();
        this.m_inventoryChanged = true;
    }

    @Override
    public void openGUI(PlayerEntity player) {
        Containers.openTurtleGUI(player, this);
    }

    @Override
    protected boolean canNameWithTag(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Apply dye
        ItemStack currentItem = player.getStackInHand(hand);
        if (!currentItem.isEmpty()) {
            if (currentItem.getItem() instanceof DyeItem) {
                // Dye to change turtle colour
                if (!this.getWorld().isClient) {
                    DyeColor dye = ((DyeItem) currentItem.getItem()).getColor();
                    if (this.m_brain.getDyeColour() != dye) {
                        this.m_brain.setDyeColour(dye);
                        if (!player.isCreative()) {
                            currentItem.decrement(1);
                        }
                    }
                }
                return true;
            } else if (currentItem.getItem() == Items.WATER_BUCKET && this.m_brain.getColour() != -1) {
                // Water to remove turtle colour
                if (!this.getWorld().isClient) {
                    if (this.m_brain.getColour() != -1) {
                        this.m_brain.setColour(-1);
                        if (!player.isCreative()) {
                            player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                            player.inventory.markDirty();
                        }
                    }
                }
                return true;
            }
        }

        // Open GUI or whatever
        return super.onActivate(player, hand, hit);
    }

    @Override
    public void onNeighbourChange(@Nonnull BlockPos neighbour) {
        if (this.m_moveState == MoveState.NOT_MOVED) {
            super.onNeighbourChange(neighbour);
        }
    }

    @Override
    public void onNeighbourTileEntityChange(@Nonnull BlockPos neighbour) {
        if (this.m_moveState == MoveState.NOT_MOVED) {
            super.onNeighbourTileEntityChange(neighbour);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.m_brain.update();
        synchronized (this.m_inventory) {
            if (!this.getWorld().isClient && this.m_inventoryChanged) {
                ServerComputer computer = this.getServerComputer();
                if (computer != null) {
                    computer.queueEvent("turtle_inventory");
                }

                this.m_inventoryChanged = false;
                for (int n = 0; n < this.size(); n++) {
                    this.m_previousInventory.set(n, InventoryUtil.copyItem(this.getStack(n)));
                }
            }
        }
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
    }

    @Nonnull
    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        // Write inventory
        ListTag nbttaglist = new ListTag();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!this.m_inventory.get(i)
                                 .isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) i);
                this.m_inventory.get(i)
                                .toTag(tag);
                nbttaglist.add(tag);
            }
        }
        nbt.put("Items", nbttaglist);

        // Write brain
        nbt = this.m_brain.writeToNBT(nbt);

        return super.toTag(nbt);
    }

    // IDirectionalTile

    @Override
    public void fromTag(CompoundTag nbt) {
        super.fromTag(nbt);

        // Read inventory
        ListTag nbttaglist = nbt.getList("Items", NBTUtil.TAG_COMPOUND);
        this.m_inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        this.m_previousInventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < nbttaglist.size(); i++) {
            CompoundTag tag = nbttaglist.getCompound(i);
            int slot = tag.getByte("Slot") & 0xff;
            if (slot < this.size()) {
                this.m_inventory.set(slot, ItemStack.fromTag(tag));
                this.m_previousInventory.set(slot, InventoryUtil.copyItem(this.m_inventory.get(slot)));
            }
        }

        // Read state
        this.m_brain.readFromNBT(nbt);
    }

    @Override
    protected boolean isPeripheralBlockedOnSide(ComputerSide localSide) {
        return this.hasPeripheralUpgradeOnSide(localSide);
    }

    // ITurtleTile

    @Override
    public Direction getDirection() {
        return this.getCachedState().get(BlockTurtle.FACING);
    }

    @Override
    protected ServerComputer createComputer(int instanceID, int id) {
        ServerComputer computer = new ServerComputer(this.getWorld(),
                                                     id, this.m_label,
                                                     instanceID, this.getFamily(),
                                                     ComputerCraft.terminalWidth_turtle,
                                                     ComputerCraft.terminalHeight_turtle);
        computer.setPosition(this.getPos());
        computer.addAPI(new TurtleAPI(computer.getAPIEnvironment(), this.getAccess()));
        this.m_brain.setupComputer(computer);
        return computer;
    }

    @Override
    public ComputerProxy createProxy() {
        return this.m_brain.getProxy();
    }

    @Override
    protected void writeDescription(@Nonnull CompoundTag nbt) {
        super.writeDescription(nbt);
        this.m_brain.writeDescription(nbt);
    }

    @Override
    protected void readDescription(@Nonnull CompoundTag nbt) {
        super.readDescription(nbt);
        this.m_brain.readDescription(nbt);
        this.updateBlock();
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral(@Nonnull Direction side) {
        return this.hasMoved() ? null : new ComputerPeripheral("turtle", this.createProxy());
    }

    public void setDirection(Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) {
            dir = Direction.NORTH;
        }
        this.world.setBlockState(this.pos,
                                 this.getCachedState().with(BlockTurtle.FACING, dir));
        this.updateOutput();
        this.updateInput();
        this.onTileEntityChange();
    }

    public void onTileEntityChange() {
        super.markDirty();
    }

    // IInventory

    private boolean hasPeripheralUpgradeOnSide(ComputerSide side) {
        ITurtleUpgrade upgrade;
        switch (side) {
        case RIGHT:
            upgrade = this.getUpgrade(TurtleSide.Right);
            break;
        case LEFT:
            upgrade = this.getUpgrade(TurtleSide.Left);
            break;
        default:
            return false;
        }
        return upgrade != null && upgrade.getType()
                                         .isPeripheral();
    }

    @Override
    protected double getInteractRange(PlayerEntity player) {
        return 12.0;
    }

    public void notifyMoveStart() {
        if (this.m_moveState == MoveState.NOT_MOVED) {
            this.m_moveState = MoveState.IN_PROGRESS;
        }
    }

    public void notifyMoveEnd() {
        // MoveState.MOVED is final
        if (this.m_moveState == MoveState.IN_PROGRESS) {
            this.m_moveState = MoveState.NOT_MOVED;
        }
    }

    @Override
    public int getColour() {
        return this.m_brain.getColour();
    }

    @Override
    public Identifier getOverlay() {
        return this.m_brain.getOverlay();
    }

    @Override
    public ITurtleUpgrade getUpgrade(TurtleSide side) {
        return this.m_brain.getUpgrade(side);
    }

    @Override
    public ITurtleAccess getAccess() {
        return this.m_brain;
    }

    @Override
    public Vec3d getRenderOffset(float f) {
        return this.m_brain.getRenderOffset(f);
    }

    @Override
    public float getRenderYaw(float f) {
        return this.m_brain.getVisualYaw(f);
    }

    @Override
    public float getToolRenderAngle(TurtleSide side, float f) {
        return this.m_brain.getToolRenderAngle(side, f);
    }

    // Networking stuff

    public void setOwningPlayer(GameProfile player) {
        this.m_brain.setOwningPlayer(player);
        this.markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        synchronized (this.m_inventory) {
            if (!this.m_inventoryChanged) {
                for (int n = 0; n < this.size(); n++) {
                    if (!ItemStack.areEqual(this.getStack(n), this.m_previousInventory.get(n))) {
                        this.m_inventoryChanged = true;
                        break;
                    }
                }
            }
        }
    }

    // Privates

    @Override
    public void clear() {
        synchronized (this.m_inventory) {
            boolean changed = false;
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (!this.m_inventory.get(i)
                                     .isEmpty()) {
                    this.m_inventory.set(i, ItemStack.EMPTY);
                    changed = true;
                }
            }
            if (changed) {
                this.onInventoryDefinitelyChanged();
            }
        }
    }

    public void transferStateFrom(TileTurtle copy) {
        super.transferStateFrom(copy);
        this.m_inventory = copy.m_inventory;
        this.m_previousInventory = copy.m_previousInventory;
        this.m_inventoryChanged = copy.m_inventoryChanged;
        this.m_brain = copy.m_brain;
        this.m_brain.setOwner(this);
        copy.m_moveState = MoveState.MOVED;
    }

    enum MoveState {
        NOT_MOVED, IN_PROGRESS, MOVED
    }
}
