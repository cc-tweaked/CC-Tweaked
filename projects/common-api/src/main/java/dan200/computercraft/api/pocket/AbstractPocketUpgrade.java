// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;


/**
 * A base class for {@link IPocketUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractPocketUpgrade implements IPocketUpgrade {
    private final ResourceLocation id;
    private final String adjective;
    private final @Nullable ItemStack stack;
    private final Ingredient ingredient;

    protected AbstractPocketUpgrade(ResourceLocation id, String adjective, ItemStack stack) {
        this.id = id;
        this.adjective = adjective;
        this.stack = stack;
        this.ingredient = Ingredient.of(stack);
    }

    protected AbstractPocketUpgrade(ResourceLocation id, String adjective, Ingredient ingredient) {
        this.id = id;
        this.adjective = adjective;
        this.stack = null; // We need to lazily resolve the ingredient, so don't set the stack.
        this.ingredient = ingredient;
    }

    protected AbstractPocketUpgrade(ResourceLocation id, ItemStack stack) {
        this(id, UpgradeBase.getDefaultAdjective(id), stack);
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

    public abstract static class StackAware extends AbstractPocketUpgrade {
        protected StackAware(ResourceLocation id, String adjective, Ingredient ingredient) {
            super(id, adjective, ingredient);
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
