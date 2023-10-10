// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Indicates if an equipped turtle item will consume durability.
 *
 * @see TurtleUpgradeDataProvider.ToolBuilder#consumeDurability(TurtleToolDurability)
 */
public enum TurtleToolDurability implements StringRepresentable {
    /**
     * The equipped tool always consumes durability when using.
     */
    ALWAYS("always"),

    /**
     * The equipped tool consumes durability if it is {@linkplain ItemStack#isEnchanted() enchanted} or has
     * {@linkplain ItemStack#getAttributeModifiers(EquipmentSlot) custom attribute modifiers}.
     */
    WHEN_ENCHANTED("when_enchanted"),

    /**
     * The equipped tool never consumes durability. Tools which have been damaged cannot be used as upgrades.
     */
    NEVER("never");

    private final String serialisedName;

    /**
     * The codec which may be used for serialising/deserialising {@link TurtleToolDurability}s.
     */
    @SuppressWarnings("deprecation")
    public static final StringRepresentable.EnumCodec<TurtleToolDurability> CODEC = StringRepresentable.fromEnum(TurtleToolDurability::values);

    TurtleToolDurability(String serialisedName) {
        this.serialisedName = serialisedName;
    }

    @Override
    public String getSerializedName() {
        return serialisedName;
    }
}
