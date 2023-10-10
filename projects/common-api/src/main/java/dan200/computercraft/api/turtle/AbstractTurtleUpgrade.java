// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.upgrades.UpgradeBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


/**
 * A base class for {@link ITurtleUpgrade}s.
 * <p>
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final ResourceLocation id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    protected AbstractTurtleUpgrade(ResourceLocation id, TurtleUpgradeType type, String adjective, ItemStack stack) {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
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
    public final ItemStack getCraftingItem() {
        return stack;
    }
}
