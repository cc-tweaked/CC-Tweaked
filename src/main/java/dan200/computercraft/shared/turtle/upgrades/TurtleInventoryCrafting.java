/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class TurtleInventoryCrafting extends CraftingInventory {
    private ITurtleAccess m_turtle;
    private int m_xStart;
    private int m_yStart;

    @SuppressWarnings ("ConstantConditions")
    public TurtleInventoryCrafting(ITurtleAccess turtle) {
        // Passing null in here is evil, but we don't have a container present. We override most methods in order to
        // avoid throwing any NPEs.
        super(null, 0, 0);
        this.m_turtle = turtle;
        this.m_xStart = 0;
        this.m_yStart = 0;
    }

    @Nullable
    private Recipe<CraftingInventory> tryCrafting(int xStart, int yStart) {
        this.m_xStart = xStart;
        this.m_yStart = yStart;

        // Check the non-relevant parts of the inventory are empty
        for (int x = 0; x < TileTurtle.INVENTORY_WIDTH; x++) {
            for (int y = 0; y < TileTurtle.INVENTORY_HEIGHT; y++) {
                if (x < this.m_xStart || x >= this.m_xStart + 3 || y < this.m_yStart || y >= this.m_yStart + 3) {
                    if (!this.m_turtle.getInventory()
                                      .getStack(x + y * TileTurtle.INVENTORY_WIDTH)
                                      .isEmpty()) {
                        return null;
                    }
                }
            }
        }

        // Check the actual crafting
        return this.m_turtle.getWorld()
                            .getRecipeManager()
                            .getFirstMatch(RecipeType.CRAFTING, this, this.m_turtle.getWorld())
                            .orElse(null);
    }

    @Nullable
    public List<ItemStack> doCrafting(World world, int maxCount) {
        if (world.isClient || !(world instanceof ServerWorld)) {
            return null;
        }

        // Find out what we can craft
        Recipe<CraftingInventory> recipe = this.tryCrafting(0, 0);
        if (recipe == null) {
            recipe = this.tryCrafting(0, 1);
        }
        if (recipe == null) {
            recipe = this.tryCrafting(1, 0);
        }
        if (recipe == null) {
            recipe = this.tryCrafting(1, 1);
        }
        if (recipe == null) {
            return null;
        }

        // Special case: craft(0) just returns an empty list if crafting was possible
        if (maxCount == 0) {
            return Collections.emptyList();
        }

        TurtlePlayer player = TurtlePlayer.get(this.m_turtle);

        ArrayList<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < maxCount && recipe.matches(this, world); i++) {
            ItemStack result = recipe.craft(this);
            if (result.isEmpty()) {
                break;
            }
            results.add(result);

            result.onCraft(world, player, result.getCount());
            DefaultedList<ItemStack> remainders = recipe.getRemainingStacks(this);

            for (int slot = 0; slot < remainders.size(); slot++) {
                ItemStack existing = this.getStack(slot);
                ItemStack remainder = remainders.get(slot);

                if (!existing.isEmpty()) {
                    this.removeStack(slot, 1);
                    existing = this.getStack(slot);
                }

                if (remainder.isEmpty()) {
                    continue;
                }

                // Either update the current stack or add it to the remainder list (to be inserted into the inventory
                // afterwards).
                if (existing.isEmpty()) {
                    this.setStack(slot, remainder);
                } else if (ItemStack.areItemsEqualIgnoreDamage(existing, remainder) && ItemStack.areTagsEqual(existing, remainder)) {
                    remainder.increment(existing.getCount());
                    this.setStack(slot, remainder);
                } else {
                    results.add(remainder);
                }
            }
        }

        return results;
    }

    @Override
    public int getMaxCountPerStack() {
        return this.m_turtle.getInventory()
                            .getMaxCountPerStack();
    }    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public boolean isValid(int i, @Nonnull ItemStack stack) {
        i = this.modifyIndex(i);
        return this.m_turtle.getInventory()
                            .isValid(i, stack);
    }    @Override
    public int getHeight() {
        return 3;
    }

    private int modifyIndex(int index) {
        int x = this.m_xStart + index % this.getWidth();
        int y = this.m_yStart + index / this.getHeight();
        return x >= 0 && x < TileTurtle.INVENTORY_WIDTH && y >= 0 && y < TileTurtle.INVENTORY_HEIGHT ? x + y * TileTurtle.INVENTORY_WIDTH : -1;
    }

    // IInventory implementation

    @Override
    public int size() {
        return this.getWidth() * this.getHeight();
    }

    @Nonnull
    @Override
    public ItemStack getStack(int i) {
        i = this.modifyIndex(i);
        return this.m_turtle.getInventory()
                            .getStack(i);
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int i) {
        i = this.modifyIndex(i);
        return this.m_turtle.getInventory()
                            .removeStack(i);
    }

    @Nonnull
    @Override
    public ItemStack removeStack(int i, int size) {
        i = this.modifyIndex(i);
        return this.m_turtle.getInventory()
                            .removeStack(i, size);
    }

    @Override
    public void setStack(int i, @Nonnull ItemStack stack) {
        i = this.modifyIndex(i);
        this.m_turtle.getInventory()
                     .setStack(i, stack);
    }



    @Override
    public void markDirty() {
        this.m_turtle.getInventory()
                     .markDirty();
    }

    @Override
    public boolean canPlayerUse(@Nonnull PlayerEntity player) {
        return true;
    }



    @Override
    public void clear() {
        for (int i = 0; i < this.size(); i++) {
            int j = this.modifyIndex(i);
            this.m_turtle.getInventory()
                         .setStack(j, ItemStack.EMPTY);
        }
    }
}
