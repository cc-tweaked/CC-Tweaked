// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;


/**
 * A base class for {@link IPocketUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractPocketUpgrade implements IPocketUpgrade {
    private final Component adjective;
    private final Ingredient ingredient;

    protected AbstractPocketUpgrade(Component adjective, Ingredient ingredient) {
        this.adjective = adjective;
        this.ingredient = ingredient;
    }

    protected AbstractPocketUpgrade(String adjective, Ingredient ingredient) {
        this(Component.translatable(adjective), ingredient);
    }

    @Override
    public final Component getAdjective() {
        return adjective;
    }

    @Override
    public final Ingredient getCraftingIngredient() {
        return ingredient;
    }
}
