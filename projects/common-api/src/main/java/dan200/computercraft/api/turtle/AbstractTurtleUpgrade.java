// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;


/**
 * A base class for {@link ITurtleUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final ResourceLocation id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final @Nullable ItemStack stack;
    private final Ingredient ingredient;

    protected AbstractTurtleUpgrade(ResourceLocation id, TurtleUpgradeType type, String adjective, ItemStack stack) {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
        this.ingredient = Ingredient.of(stack);
    }

    protected AbstractTurtleUpgrade(ResourceLocation id, TurtleUpgradeType type, String adjective, Ingredient ingredient) {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = null; // We need to lazily resolve the ingredient, so don't set the stack.
        this.ingredient = ingredient;
    }

    protected AbstractTurtleUpgrade(ResourceLocation id, TurtleUpgradeType type, ItemStack stack) {
        this(id, type, UpgradeBase.getDefaultAdjective(id), stack);
    }

    @Override
    public final ResourceLocation getUpgradeID() {
        return id;
    }

    @Override
    public final String getUnlocalisedAdjective() {
        return adjective;
    }

    @Override
    public final TurtleUpgradeType getType() {
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
     * {@link ITurtleAccess#getUpgradeHandle(TurtleSide)} (and not {@link ITurtleAccess#getUpgradeNBTData(TurtleSide)}
     * or {@link ITurtleAccess#updateUpgradeNBTData(TurtleSide)}).
     *
     * @since 1.121.0
     */
    public abstract static class StackAware extends AbstractTurtleUpgrade {
        protected StackAware(ResourceLocation id, TurtleUpgradeType type, String adjective, Ingredient ingredient) {
            super(id, type, adjective, ingredient);
        }

        @Override
        @Deprecated
        public final CompoundTag getUpgradeData(ItemStack stack) {
            return super.getUpgradeData(stack);
        }

        @Override
        public boolean isItemSuitable(ItemStack stack) {
            return true;
        }
    }
}
