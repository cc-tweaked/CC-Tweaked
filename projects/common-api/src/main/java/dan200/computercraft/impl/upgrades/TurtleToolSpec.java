// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.turtle.TurtleToolDurability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * The template for a turtle tool.
 *
 * @param adjective         The adjective for this tool.
 * @param craftItem         The item used to craft this tool.
 * @param toolItem          The actual tool used.
 * @param damageMultiplier  The damage multiplier for this tool.
 * @param allowEnchantments Whether to allow enchantments.
 * @param consumeDurability When to consume durability.
 * @param breakable         The items breakable by this tool.
 */
public record TurtleToolSpec(
    String adjective,
    Optional<Item> craftItem,
    Item toolItem,
    float damageMultiplier,
    boolean allowEnchantments,
    TurtleToolDurability consumeDurability,
    Optional<TagKey<Block>> breakable
) {
    public static final float DEFAULT_DAMAGE_MULTIPLIER = 3.0f;

    public static final MapCodec<TurtleToolSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("adjective").forGetter(TurtleToolSpec::adjective),
        BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("craftingItem").forGetter(TurtleToolSpec::craftItem),
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(TurtleToolSpec::toolItem),
        Codec.FLOAT.optionalFieldOf("damageMultiplier", DEFAULT_DAMAGE_MULTIPLIER).forGetter(TurtleToolSpec::damageMultiplier),
        Codec.BOOL.optionalFieldOf("allowEnchantments", false).forGetter(TurtleToolSpec::allowEnchantments),
        TurtleToolDurability.CODEC.optionalFieldOf("consumeDurability", TurtleToolDurability.NEVER).forGetter(TurtleToolSpec::consumeDurability),
        TagKey.codec(Registries.BLOCK).optionalFieldOf("breakable").forGetter(TurtleToolSpec::breakable)
    ).apply(instance, TurtleToolSpec::new));
}
