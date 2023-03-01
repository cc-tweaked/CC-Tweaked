// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


/**
 * A base class for {@link IPocketUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractPocketUpgrade implements IPocketUpgrade {
    private final ResourceLocation id;
    private final String adjective;
    private final ItemStack stack;

    protected AbstractPocketUpgrade(ResourceLocation id, String adjective, ItemStack stack) {
        this.id = id;
        this.adjective = adjective;
        this.stack = stack;
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
    public final ItemStack getCraftingItem() {
        return stack;
    }
}
