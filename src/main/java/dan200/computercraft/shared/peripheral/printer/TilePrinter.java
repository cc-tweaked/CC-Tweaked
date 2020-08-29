/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DefaultPropertyDelegate;
import dan200.computercraft.shared.util.DefaultSidedInventory;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class TilePrinter extends TileGeneric implements DefaultSidedInventory, IPeripheralTile, DefaultPropertyDelegate, Nameable {
    public static final NamedBlockEntityType<TilePrinter> FACTORY = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID, "printer"),
                                                                                                TilePrinter::new);
    public static final int PROPERTY_SIZE = 1;
    public static final int PROPERTY_PRINTING = 0;
    public static final int INVENTORY_SIZE = 13;
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_PRINTING = "Printing";
    private static final String NBT_PAGE_TITLE = "PageTitle";
    private static final int[] BOTTOM_SLOTS = new int[] {
        7,
        8,
        9,
        10,
        11,
        12
    };
    private static final int[] TOP_SLOTS = new int[] {
        1,
        2,
        3,
        4,
        5,
        6
    };
    private static final int[] SIDE_SLOTS = new int[] {0};
    private final DefaultedList<ItemStack> m_inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final ItemStorage m_itemHandlerAll = ItemStorage.wrap(this);
    private final Terminal m_page = new Terminal(ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE);
    Text customName;
    private String m_pageTitle = "";
    private boolean m_printing = false;

    private TilePrinter() {
        super(FACTORY);
    }

    @Override
    public void destroy() {
        this.ejectContents();
    }

    @Override
    public boolean onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking()) {
            return false;
        }

        if (!this.getWorld().isClient) {
            Containers.openPrinterGUI(player, this);
        }
        return true;
    }

    @Override
    public void readDescription(@Nonnull CompoundTag nbt) {
        super.readDescription(nbt);
        this.customName = nbt.contains(NBT_NAME) ? LiteralText.Serializer.fromJson(nbt.getString(NBT_NAME)) : null;
        this.updateBlock();
    }

    @Override
    protected void writeDescription(@Nonnull CompoundTag nbt) {
        super.writeDescription(nbt);
        if (this.customName != null) {
            nbt.putString(NBT_NAME, LiteralText.Serializer.toJson(this.customName));
        }
    }

    private void ejectContents() {
        synchronized (this.m_inventory) {
            for (int i = 0; i < 13; i++) {
                ItemStack stack = this.m_inventory.get(i);
                if (!stack.isEmpty()) {
                    // Remove the stack from the inventory
                    this.setStack(i, ItemStack.EMPTY);

                    // Spawn the item in the world
                    BlockPos pos = this.getPos();
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 0.75;
                    double z = pos.getZ() + 0.5;
                    WorldUtil.dropItemStack(stack, this.getWorld(), x, y, z);
                }
            }
        }
    }

    private void updateBlockState() {
        boolean top = false, bottom = false;
        synchronized (this.m_inventory) {
            for (int i = 1; i < 7; i++) {
                ItemStack stack = this.m_inventory.get(i);
                if (!stack.isEmpty() && isPaper(stack)) {
                    top = true;
                    break;
                }
            }
            for (int i = 7; i < 13; i++) {
                ItemStack stack = this.m_inventory.get(i);
                if (!stack.isEmpty() && isPaper(stack)) {
                    bottom = true;
                    break;
                }
            }
        }

        this.updateBlockState(top, bottom);
    }

    private static boolean isPaper(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.PAPER || (item instanceof ItemPrintout && ((ItemPrintout) item).getType() == ItemPrintout.Type.PAGE);
    }

    private void updateBlockState(boolean top, boolean bottom) {
        if (this.removed) {
            return;
        }

        BlockState state = this.getCachedState();
        if (state.get(BlockPrinter.TOP) == top & state.get(BlockPrinter.BOTTOM) == bottom) {
            return;
        }

        this.getWorld().setBlockState(this.getPos(),
                                      state.with(BlockPrinter.TOP, top)
                                      .with(BlockPrinter.BOTTOM, bottom));
    }

    @Override
    public void fromTag(CompoundTag nbt) {
        super.fromTag(nbt);

        this.customName = nbt.contains(NBT_NAME) ? LiteralText.Serializer.fromJson(nbt.getString(NBT_NAME)) : null;

        // Read page
        synchronized (this.m_page) {
            this.m_printing = nbt.getBoolean(NBT_PRINTING);
            this.m_pageTitle = nbt.getString(NBT_PAGE_TITLE);
            this.m_page.readFromNBT(nbt);
        }

        // Read inventory
        synchronized (this.m_inventory) {
            Inventories.fromTag(nbt, this.m_inventory);
        }
    }

    @Nonnull
    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        if (this.customName != null) {
            nbt.putString(NBT_NAME, LiteralText.Serializer.toJson(this.customName));
        }

        // Write page
        synchronized (this.m_page) {
            nbt.putBoolean(NBT_PRINTING, this.m_printing);
            nbt.putString(NBT_PAGE_TITLE, this.m_pageTitle);
            this.m_page.writeToNBT(nbt);
        }

        // Write inventory
        synchronized (this.m_inventory) {
            Inventories.toTag(nbt, this.m_inventory);
        }

        return super.toTag(nbt);
    }

    // Inventory implementation
    @Override
    public int size() {
        return this.m_inventory.size();
    }

    @Override
    public int size() {
        return PROPERTY_SIZE;
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
    public ItemStack getStack(int i) {
        return this.m_inventory.get(i);
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int i, int j) {
        synchronized (this.m_inventory) {
            if (this.m_inventory.get(i)
                                .isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (this.m_inventory.get(i)
                                .getCount() <= j) {
                ItemStack itemstack = this.m_inventory.get(i);
                this.m_inventory.set(i, ItemStack.EMPTY);
                this.markDirty();
                this.updateBlockState();
                return itemstack;
            }

            ItemStack part = this.m_inventory.get(i)
                                             .split(j);
            if (this.m_inventory.get(i)
                                .isEmpty()) {
                this.m_inventory.set(i, ItemStack.EMPTY);
                this.updateBlockState();
            }
            this.markDirty();
            return part;
        }
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int i) {
        synchronized (this.m_inventory) {
            ItemStack result = this.m_inventory.get(i);
            this.m_inventory.set(i, ItemStack.EMPTY);
            this.markDirty();
            this.updateBlockState();
            return result;
        }
    }

    // ISidedInventory implementation

    @Override
    public void setStack(int i, @Nonnull ItemStack stack) {
        synchronized (this.m_inventory) {
            this.m_inventory.set(i, stack);
            this.markDirty();
            this.updateBlockState();
        }
    }

    // IPeripheralTile implementation

    @Override
    public boolean canPlayerUse(PlayerEntity playerEntity) {
        return this.isUsable(playerEntity, false);
    }

    @Override
    public boolean isValid(int slot, @Nonnull ItemStack stack) {
        if (slot == 0) {
            return isInk(stack);
        } else if (slot >= TOP_SLOTS[0] && slot <= TOP_SLOTS[TOP_SLOTS.length - 1]) {
            return isPaper(stack);
        } else {
            return false;
        }
    }

    private static boolean isInk(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof DyeItem;
    }

    @Override
    public void clear() {
        synchronized (this.m_inventory) {
            for (int i = 0; i < this.m_inventory.size(); i++) {
                this.m_inventory.set(i, ItemStack.EMPTY);
            }
            this.markDirty();
            this.updateBlockState();
        }
    }

    @Override
    public int[] getAvailableSlots(@Nonnull Direction side) {
        switch (side) {
        case DOWN: // Bottom (Out tray)
            return BOTTOM_SLOTS;
        case UP: // Top (In tray)
            return TOP_SLOTS;
        default: // Sides (Ink)
            return SIDE_SLOTS;
        }
    }

    @Override
    public IPeripheral getPeripheral(@Nonnull Direction side) {
        return new PrinterPeripheral(this);
    }

    public Terminal getCurrentPage() {
        return this.m_printing ? this.m_page : null;
    }

    public boolean startNewPage() {
        synchronized (this.m_inventory) {
            if (!this.canInputPage()) {
                return false;
            }
            if (this.m_printing && !this.outputPage()) {
                return false;
            }
            return this.inputPage();
        }
    }

    public boolean endCurrentPage() {
        synchronized (this.m_inventory) {
            if (this.m_printing && this.outputPage()) {
                return true;
            }
        }
        return false;
    }

    private boolean outputPage() {
        synchronized (this.m_page) {
            int height = this.m_page.getHeight();
            String[] lines = new String[height];
            String[] colours = new String[height];
            for (int i = 0; i < height; i++) {
                lines[i] = this.m_page.getLine(i)
                                      .toString();
                colours[i] = this.m_page.getTextColourLine(i)
                                        .toString();
            }

            ItemStack stack = ItemPrintout.createSingleFromTitleAndText(this.m_pageTitle, lines, colours);
            synchronized (this.m_inventory) {
                for (int slot : BOTTOM_SLOTS) {
                    if (this.m_inventory.get(slot)
                                        .isEmpty()) {
                        this.setStack(slot, stack);
                        this.m_printing = false;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public int getInkLevel() {
        synchronized (this.m_inventory) {
            ItemStack inkStack = this.m_inventory.get(0);
            return isInk(inkStack) ? inkStack.getCount() : 0;
        }
    }

    public int getPaperLevel() {
        int count = 0;
        synchronized (this.m_inventory) {
            for (int i = 1; i < 7; i++) {
                ItemStack paperStack = this.m_inventory.get(i);
                if (!paperStack.isEmpty() && isPaper(paperStack)) {
                    count += paperStack.getCount();
                }
            }
        }
        return count;
    }

    public void setPageTitle(String title) {
        if (this.m_printing) {
            this.m_pageTitle = title;
        }
    }

    private boolean canInputPage() {
        synchronized (this.m_inventory) {
            ItemStack inkStack = this.m_inventory.get(0);
            return !inkStack.isEmpty() && isInk(inkStack) && this.getPaperLevel() > 0;
        }
    }

    private boolean inputPage() {
        synchronized (this.m_inventory) {
            ItemStack inkStack = this.m_inventory.get(0);
            if (!isInk(inkStack)) {
                return false;
            }

            for (int i = 1; i < 7; i++) {
                ItemStack paperStack = this.m_inventory.get(i);
                if (!paperStack.isEmpty() && isPaper(paperStack)) {
                    // Setup the new page
                    DyeColor dye = ColourUtils.getStackColour(inkStack);
                    this.m_page.setTextColour(dye != null ? dye.getId() : 15);

                    this.m_page.clear();
                    if (paperStack.getItem() instanceof ItemPrintout) {
                        this.m_pageTitle = ItemPrintout.getTitle(paperStack);
                        String[] text = ItemPrintout.getText(paperStack);
                        String[] textColour = ItemPrintout.getColours(paperStack);
                        for (int y = 0; y < this.m_page.getHeight(); y++) {
                            this.m_page.setLine(y, text[y], textColour[y], "");
                        }
                    } else {
                        this.m_pageTitle = "";
                    }
                    this.m_page.setCursorPos(0, 0);

                    // Decrement ink
                    inkStack.decrement(1);
                    if (inkStack.isEmpty()) {
                        this.m_inventory.set(0, ItemStack.EMPTY);
                    }

                    // Decrement paper
                    paperStack.decrement(1);
                    if (paperStack.isEmpty()) {
                        this.m_inventory.set(i, ItemStack.EMPTY);
                        this.updateBlockState();
                    }

                    this.markDirty();
                    this.m_printing = true;
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public int get(int property) {
        if (property == PROPERTY_PRINTING) {
            return this.isPrinting() ? 1 : 0;
        }
        return 0;
    }

    public boolean isPrinting() {
        return this.m_printing;
    }

    @Nonnull
    @Override
    public Text getName() {
        return this.customName != null ? this.customName : this.getCachedState().getBlock()
                                                               .getName();
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return this.customName;
    }
}
