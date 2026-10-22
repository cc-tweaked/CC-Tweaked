// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;


/**
 * A base class for {@link ITurtleUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final TurtleUpgradeType type;
    private final Component adjective;
    private final @Nullable ItemStack stack;
    private final Ingredient ingredient;

    protected AbstractTurtleUpgrade(TurtleUpgradeType type, Component adjective, ItemStack stack) {
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
        this.ingredient = Ingredient.of(stack);
    }

    protected AbstractTurtleUpgrade(TurtleUpgradeType type, Component adjective, Ingredient ingredient) {
        this.type = type;
        this.adjective = adjective;
        this.stack = null; // We need to lazily resolve the ingredient, so don't set the stack.
        this.ingredient = ingredient;
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
    @Deprecated
    public final ItemStack getCraftingItem() {
        if (stack != null) return stack;

        var items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    @Override
    public final Ingredient getCraftingIngredient() {
        return ingredient;
    }

    /**
     * A version of {@link AbstractTurtleUpgrade} which is aware of the stack the upgrade was equipped with.
     * <p>
     * Upgrades implementing this method should only interact with the upgrade via
     * {@link ITurtleAccess#getUpgradeHandle(TurtleSide)} (and not {@link ITurtleAccess#getUpgradeData(TurtleSide)}
     * or {@link ITurtleAccess#setUpgradeData(TurtleSide, DataComponentPatch)}).
     *
     * @since 1.121.0
     */
    public abstract static class StackAware extends AbstractTurtleUpgrade {
        protected StackAware(TurtleUpgradeType type, Component adjective, Ingredient ingredient) {
            super(type, adjective, ingredient);
        }

        protected StackAware(TurtleUpgradeType type, String adjective, Ingredient ingredient) {
            this(type, Component.translatable(adjective), ingredient);
        }

        @Override
        @Deprecated
        public final DataComponentPatch getUpgradeData(ItemStack stack) {
            return super.getUpgradeData(stack);
        }

        @Override
        public boolean isItemSuitable(ItemStack stack) {
            return true;
        }
    }
}
