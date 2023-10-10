// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.inventory;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.TurtleUpgrades;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A fake {@link Container} which exposes the {@linkplain ITurtleAccess#getUpgrade(TurtleSide) upgrades} a turtle has.
 *
 * @see TurtleMenu
 * @see UpgradeSlot
 */
class UpgradeContainer implements Container {
    private static final int SIZE = 2;

    private final ITurtleAccess turtle;

    private final List<UpgradeData<ITurtleUpgrade>> lastUpgrade = Arrays.asList(null, null);
    private final NonNullList<ItemStack> lastStack = NonNullList.withSize(2, ItemStack.EMPTY);

    UpgradeContainer(ITurtleAccess turtle) {
        this.turtle = turtle;
    }

    private TurtleSide getSide(int slot) {
        return switch (slot) {
            case 0 -> TurtleSide.LEFT;
            case 1 -> TurtleSide.RIGHT;
            default -> throw new IllegalArgumentException("Invalid slot " + slot);
        };
    }

    @Override
    public ItemStack getItem(int slot) {
        var side = getSide(slot);
        var upgrade = turtle.getUpgradeWithData(side);
        if (upgrade == null) return ItemStack.EMPTY;

        // We don't want to return getUpgradeItem directly here, as we'd end up recreating the stack each tick. To
        // avoid that, we maintain a simple cache.
        if (upgrade.equals(lastUpgrade.get(slot))) return lastStack.get(slot);

        return setUpgradeStack(slot, upgrade);
    }

    private ItemStack setUpgradeStack(int slot, @Nullable UpgradeData<ITurtleUpgrade> upgrade) {
        var stack = upgrade == null ? ItemStack.EMPTY : upgrade.getUpgradeItem();
        lastUpgrade.set(slot, UpgradeData.copyOf(upgrade));
        lastStack.set(slot, stack);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        var upgrade = TurtleUpgrades.instance().get(itemStack);
        turtle.setUpgradeWithData(getSide(slot), upgrade);
        setUpgradeStack(slot, upgrade);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (var i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count <= 0 ? ItemStack.EMPTY : removeItemNoUpdate(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var current = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (var i = 0; i < SIZE; i++) setItem(i, ItemStack.EMPTY);
    }
}
