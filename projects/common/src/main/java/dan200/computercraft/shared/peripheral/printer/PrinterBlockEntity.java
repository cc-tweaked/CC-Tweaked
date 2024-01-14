// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.AbstractContainerBlockEntity;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.container.BasicWorldlyContainer;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class PrinterBlockEntity extends AbstractContainerBlockEntity implements BasicWorldlyContainer {
    private static final String NBT_PRINTING = "Printing";
    private static final String NBT_PAGE_TITLE = "PageTitle";

    static final int SLOTS = 13;

    private static final int[] BOTTOM_SLOTS = new int[]{ 7, 8, 9, 10, 11, 12 };
    private static final int[] TOP_SLOTS = new int[]{ 1, 2, 3, 4, 5, 6 };
    private static final int[] SIDE_SLOTS = new int[]{ 0 };

    private final PrinterPeripheral peripheral = new PrinterPeripheral(this);
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    private final NetworkedTerminal page = new NetworkedTerminal(PrintoutItem.LINE_MAX_LENGTH, PrintoutItem.LINES_PER_PAGE, true);
    private String pageTitle = "";
    private boolean printing = false;

    public PrinterBlockEntity(BlockEntityType<PrinterBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IPeripheral peripheral() {
        return peripheral;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        // Read page
        synchronized (page) {
            printing = nbt.getBoolean(NBT_PRINTING);
            pageTitle = nbt.getString(NBT_PAGE_TITLE);
            page.readFromNBT(nbt);
        }

        // Read inventory
        ContainerHelper.loadAllItems(nbt, inventory);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        // Write page
        synchronized (page) {
            tag.putBoolean(NBT_PRINTING, printing);
            tag.putString(NBT_PAGE_TITLE, pageTitle);
            page.writeToNBT(tag);
        }

        // Write inventory
        ContainerHelper.saveAllItems(tag, inventory);

        super.saveAdditional(tag);
    }

    boolean isPrinting() {
        return printing;
    }

    @Override
    public NonNullList<ItemStack> getContents() {
        return inventory;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateBlockState();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) {
            return isInk(stack);
        } else if (slot >= TOP_SLOTS[0] && slot <= TOP_SLOTS[TOP_SLOTS.length - 1]) {
            return isPaper(stack);
        } else {
            return false;
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> BOTTOM_SLOTS; // Bottom (Out tray)
            case UP -> TOP_SLOTS; // Top (In tray)
            default -> SIDE_SLOTS; // Sides (Ink)
        };
    }

    @Nullable
    NetworkedTerminal getCurrentPage() {
        synchronized (page) {
            return printing ? page : null;
        }
    }

    boolean startNewPage() {
        synchronized (page) {
            if (!canInputPage()) return false;
            if (printing && !outputPage()) return false;
            return inputPage();
        }
    }

    boolean endCurrentPage() {
        synchronized (page) {
            return printing && outputPage();
        }
    }

    int getInkLevel() {
        var inkStack = inventory.get(0);
        return isInk(inkStack) ? inkStack.getCount() : 0;
    }

    int getPaperLevel() {
        var count = 0;
        for (var i = 1; i < 7; i++) {
            var paperStack = inventory.get(i);
            if (isPaper(paperStack)) count += paperStack.getCount();
        }
        return count;
    }

    void setPageTitle(String title) {
        synchronized (page) {
            if (printing) pageTitle = title;
        }
    }

    static boolean isInk(ItemStack stack) {
        return ColourUtils.getStackColour(stack) != null;
    }

    static boolean isPaper(ItemStack stack) {
        var item = stack.getItem();
        return item == Items.PAPER
            || (item instanceof PrintoutItem printout && printout.getType() == PrintoutItem.Type.PAGE);
    }

    private boolean canInputPage() {
        var inkStack = inventory.get(0);
        return !inkStack.isEmpty() && isInk(inkStack) && getPaperLevel() > 0;
    }

    private boolean inputPage() {
        var inkStack = inventory.get(0);
        var dye = ColourUtils.getStackColour(inkStack);
        if (dye == null) return false;

        for (var i = 1; i < 7; i++) {
            var paperStack = inventory.get(i);
            if (paperStack.isEmpty() || !isPaper(paperStack)) continue;

            // Setup the new page
            page.setTextColour(dye.getId());

            page.clear();
            if (paperStack.getItem() instanceof PrintoutItem) {
                pageTitle = PrintoutItem.getTitle(paperStack);
                var text = PrintoutItem.getText(paperStack);
                var textColour = PrintoutItem.getColours(paperStack);
                for (var y = 0; y < page.getHeight(); y++) {
                    page.setLine(y, text[y], textColour[y], "");
                }
            } else {
                pageTitle = "";
            }
            page.setCursorPos(0, 0);

            // Decrement ink
            inkStack.shrink(1);
            if (inkStack.isEmpty()) inventory.set(0, ItemStack.EMPTY);

            // Decrement paper
            paperStack.shrink(1);
            if (paperStack.isEmpty()) {
                inventory.set(i, ItemStack.EMPTY);
                updateBlockState();
            }

            setChanged();
            printing = true;
            return true;
        }
        return false;
    }

    private boolean outputPage() {
        var height = page.getHeight();
        var lines = new String[height];
        var colours = new String[height];
        for (var i = 0; i < height; i++) {
            lines[i] = page.getLine(i).toString();
            colours[i] = page.getTextColourLine(i).toString();
        }

        var stack = PrintoutItem.createSingleFromTitleAndText(pageTitle, lines, colours);
        for (var slot : BOTTOM_SLOTS) {
            if (inventory.get(slot).isEmpty()) {
                inventory.set(slot, stack);
                updateBlockState();
                setChanged();
                printing = false;
                return true;
            }
        }
        return false;
    }

    private void updateBlockState() {
        boolean top = false, bottom = false;
        for (var i = 1; i < 7; i++) {
            var stack = inventory.get(i);
            if (!stack.isEmpty() && isPaper(stack)) {
                top = true;
                break;
            }
        }
        for (var i = 7; i < 13; i++) {
            var stack = inventory.get(i);
            if (!stack.isEmpty() && isPaper(stack)) {
                bottom = true;
                break;
            }
        }

        updateBlockState(top, bottom);
    }

    private void updateBlockState(boolean top, boolean bottom) {
        if (remove || level == null) return;

        var state = getBlockState();
        if (state.getValue(PrinterBlock.TOP) == top && state.getValue(PrinterBlock.BOTTOM) == bottom) return;

        getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(PrinterBlock.TOP, top).setValue(PrinterBlock.BOTTOM, bottom));
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new PrinterMenu(id, inventory, this);
    }
}
