// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurtleInventoryCrafting implements CraftingContainer {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 3;
    public static final int SIZE = WIDTH * HEIGHT;

    private final ITurtleAccess turtle;
    private int xStart = 0;
    private int yStart = 0;

    @SuppressWarnings("ConstantConditions")
    public TurtleInventoryCrafting(ITurtleAccess turtle) {
        this.turtle = turtle;
    }

    @Nullable
    private Recipe<CraftingContainer> tryCrafting(int xStart, int yStart) {
        this.xStart = xStart;
        this.yStart = yStart;

        // Check the non-relevant parts of the inventory are empty
        for (var x = 0; x < TurtleBlockEntity.INVENTORY_WIDTH; x++) {
            for (var y = 0; y < TurtleBlockEntity.INVENTORY_HEIGHT; y++) {
                if (x < this.xStart || x >= this.xStart + 3 ||
                    y < this.yStart || y >= this.yStart + 3) {
                    if (!turtle.getInventory().getItem(x + y * TurtleBlockEntity.INVENTORY_WIDTH).isEmpty()) {
                        return null;
                    }
                }
            }
        }

        // Check the actual crafting
        return turtle.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this, turtle.getLevel()).orElse(null);
    }

    @Nullable
    public List<ItemStack> doCrafting(Level world, int maxCount) {
        if (world.isClientSide || !(world instanceof ServerLevel)) return null;

        // Find out what we can craft
        var recipe = tryCrafting(0, 0);
        if (recipe == null) recipe = tryCrafting(0, 1);
        if (recipe == null) recipe = tryCrafting(1, 0);
        if (recipe == null) recipe = tryCrafting(1, 1);
        if (recipe == null) return null;

        // Special case: craft(0) just returns an empty list if crafting was possible
        if (maxCount == 0) return List.of();

        var player = TurtlePlayer.get(turtle).player();

        var results = new ArrayList<ItemStack>();
        for (var i = 0; i < maxCount && recipe.matches(this, world); i++) {
            var result = recipe.assemble(this, world.registryAccess());
            if (result.isEmpty()) break;
            results.add(result);

            result.onCraftedBy(world, player, result.getCount());
            PlatformHelper.get().onItemCrafted(player, this, result);

            var remainders = PlatformHelper.get().getRecipeRemainingItems(player, recipe, this);
            for (var slot = 0; slot < remainders.size(); slot++) {
                var existing = getItem(slot);
                var remainder = remainders.get(slot);

                if (!existing.isEmpty()) {
                    removeItem(slot, 1);
                    existing = getItem(slot);
                }

                if (remainder.isEmpty()) continue;

                // Either update the current stack or add it to the remainder list (to be inserted into the inventory
                // afterwards).
                if (existing.isEmpty()) {
                    setItem(slot, remainder);
                } else if (ItemStack.isSameItemSameTags(existing, remainder)) {
                    remainder.grow(existing.getCount());
                    setItem(slot, remainder);
                } else {
                    results.add(remainder);
                }
            }
        }

        return Collections.unmodifiableList(results);
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    private int modifyIndex(int index) {
        var x = xStart + index % getWidth();
        var y = yStart + index / getHeight();
        return x >= 0 && x < TurtleBlockEntity.INVENTORY_WIDTH && y >= 0 && y < TurtleBlockEntity.INVENTORY_HEIGHT
            ? x + y * TurtleBlockEntity.INVENTORY_WIDTH
            : -1;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public ItemStack getItem(int i) {
        return turtle.getInventory().getItem(modifyIndex(i));
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return turtle.getInventory().removeItemNoUpdate(modifyIndex(i));
    }

    @Override
    public ItemStack removeItem(int i, int size) {
        return turtle.getInventory().removeItem(modifyIndex(i), size);
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        turtle.getInventory().setItem(modifyIndex(i), stack);
    }

    @Override
    public int getMaxStackSize() {
        return turtle.getInventory().getMaxStackSize();
    }

    @Override
    public void setChanged() {
        turtle.getInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack stack) {
        return turtle.getInventory().canPlaceItem(modifyIndex(i), stack);
    }

    @Override
    public void clearContent() {
        for (var i = 0; i < SIZE; i++) {
            var j = modifyIndex(i);
            turtle.getInventory().setItem(j, ItemStack.EMPTY);
        }
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < SIZE; i++) contents.accountSimpleStack(getItem(i));
    }

    @Override
    public List<ItemStack> getItems() {
        return new AbstractList<>() {
            @Override
            public ItemStack get(int index) {
                return getItem(index);
            }

            @Override
            public int size() {
                return SIZE;
            }
        };
    }
}
