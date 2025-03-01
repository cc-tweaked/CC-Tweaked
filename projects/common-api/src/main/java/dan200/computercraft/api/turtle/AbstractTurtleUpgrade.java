// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


/**
 * A base class for {@link ITurtleUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final TurtleUpgradeType type;
    private final Component adjective;
    private final ItemStack stack;

    protected AbstractTurtleUpgrade(TurtleUpgradeType type, Component adjective, ItemStack stack) {
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    protected AbstractTurtleUpgrade(TurtleUpgradeType type, String adjective, ItemStack stack) {
        this(type, Component.translatable(adjective), stack);
    }

    @Override
    public final Component getAdjective() {
        return adjective;
    }

    @Override
    public final TurtleUpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public final ItemStack getCraftingItem() {
        return stack;
    }
}
