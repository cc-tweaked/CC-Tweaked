// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;


/**
 * A base class for {@link IPocketUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractPocketUpgrade implements IPocketUpgrade {
    private final Component adjective;
    private final @Nullable ItemStackTemplate stack;
    private final Ingredient ingredient;

    protected AbstractPocketUpgrade(Component adjective, ItemStackTemplate stack) {
        this.adjective = adjective;
        this.stack = stack;
        this.ingredient = Ingredient.of(stack.item().value());
    }

    protected AbstractPocketUpgrade(Component adjective, Ingredient ingredient) {
        this.adjective = adjective;
        this.stack = null; // We need to lazily resolve the ingredient, so don't set the stack.
        this.ingredient = ingredient;
    }

    protected AbstractPocketUpgrade(String adjective, ItemStackTemplate stack) {
        this(Component.translatable(adjective), stack);
    }

    @Override
    public final Component getAdjective() {
        return adjective;
    }

    @Override
    @Deprecated
    public final ItemStackTemplate getCraftingItem() {
        if (stack != null) return stack;

        var items = ingredient.items().findFirst().orElseThrow(() -> new IllegalStateException("No item preset"));
        return new ItemStackTemplate(items.value());
    }

    @Override
    public final Ingredient getCraftingIngredient() {
        return ingredient;
    }

    /**
     * A version of {@link AbstractPocketUpgrade} which is aware of the stack the upgrade was equipped with.
     * <p>
     * Upgrades implementing this method should only interact with the upgrade via
     * {@link IPocketAccess#getUpgradeHandle()} (and not {@link IPocketAccess#getUpgradeData()} or
     * {@link IPocketAccess#setUpgradeData(DataComponentPatch)}).
     *
     * @since 1.121.0
     */
    public abstract static class StackAware extends AbstractPocketUpgrade {
        protected StackAware(Component adjective, Ingredient ingredient) {
            super(adjective, ingredient);
        }

        protected StackAware(String adjective, Ingredient ingredient) {
            this(Component.translatable(adjective), ingredient);
        }

        @Override
        @Deprecated
        public final DataComponentPatch getUpgradeData(ItemStackTemplate stack) {
            return super.getUpgradeData(stack);
        }

        @Override
        public boolean isItemSuitable(ItemStack stack) {
            return true;
        }
    }
}
