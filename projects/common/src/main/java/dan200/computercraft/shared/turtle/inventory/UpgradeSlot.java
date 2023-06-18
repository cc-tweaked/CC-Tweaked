// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.inventory;

import com.mojang.datafixers.util.Pair;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.impl.TurtleUpgrades;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * A slot in the turtle UI which holds the turtle's current upgrade.
 *
 * @see TurtleMenu
 */
public class UpgradeSlot extends Slot {
    public static final ResourceLocation LEFT_UPGRADE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/turtle_upgrade_left");
    public static final ResourceLocation RIGHT_UPGRADE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "gui/turtle_upgrade_right");

    private final TurtleSide side;

    public UpgradeSlot(Container container, TurtleSide side, int slot, int xPos, int yPos) {
        super(container, slot, xPos, yPos);
        this.side = side;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return TurtleUpgrades.instance().get(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, side == TurtleSide.LEFT ? LEFT_UPGRADE : RIGHT_UPGRADE);
    }
}
