/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * A base class for {@link ITurtleUpgrade}s.
 *
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade {
    private final Identifier id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    protected AbstractTurtleUpgrade(Identifier id, TurtleUpgradeType type, String adjective, ItemConvertible item) {
        this(id, type, adjective, new ItemStack(item));
    }

    protected AbstractTurtleUpgrade(Identifier id, TurtleUpgradeType type, String adjective, ItemStack stack) {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    protected AbstractTurtleUpgrade(Identifier id, TurtleUpgradeType type, ItemConvertible item) {
        this(id, type, new ItemStack(item));
    }

    protected AbstractTurtleUpgrade(Identifier id, TurtleUpgradeType type, ItemStack stack) {
        this(id, type, Util.createTranslationKey("upgrade", id) + ".adjective", stack);
    }


    @Nonnull
    @Override
    public final Identifier getUpgradeID() {
        return this.id;
    }

    @Nonnull
    @Override
    public final String getUnlocalisedAdjective() {
        return this.adjective;
    }

    @Nonnull
    @Override
    public final TurtleUpgradeType getType() {
        return this.type;
    }

    @Nonnull
    @Override
    public final ItemStack getCraftingItem() {
        return this.stack;
    }
}
