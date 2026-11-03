// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A base class for {@link ITurtleUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final TurtleUpgradeType type;
    private final Component adjective;
    private final Ingredient ingredient;

    /**
     * Create a new {@link ITurtleUpgrade}.
     *
     * @param type       The type of upgrade.
     * @param adjective  The adjective of the upgrade.
     * @param ingredient The ingredient the upgrade is crafted with.
     */
    protected AbstractTurtleUpgrade(TurtleUpgradeType type, Component adjective, Ingredient ingredient) {
        this.type = type;
        this.adjective = adjective;
        this.ingredient = ingredient;
    }

    /**
     * Create a new {@link ITurtleUpgrade}.
     *
     * @param type       The type of upgrade.
     * @param adjective  The adjective of the upgrade.
     * @param ingredient The ingredient the upgrade is crafted with.
     */
    protected AbstractTurtleUpgrade(TurtleUpgradeType type, String adjective, Ingredient ingredient) {
        this(type, Component.translatable(adjective), ingredient);
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
    public final Ingredient getCraftingIngredient() {
        return ingredient;
    }
}
