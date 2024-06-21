// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.impl.upgrades.TurtleToolSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A builder for custom turtle tool upgrades.
 * <p>
 * This can be used from your <a href="../upgrades/UpgradeType.html#datagen">data generator</a> code in order to
 * register turtle tools for your mod's tools.
 *
 * <h2>Example:</h2>
 * {@snippet lang = "java":
 * import net.minecraft.data.worldgen.BootstrapContext;
 * import net.minecraft.resources.ResourceLocation;
 * import net.minecraft.world.item.Items;
 *
 * public void registerTool(BootstrapContext<ITurtleUpgrade> upgrades) {
 *   TurtleToolBuilder.tool(ResourceLocation.fromNamespaceAndPath("my_mod", "wooden_pickaxe"), Items.WOODEN_PICKAXE).register(upgrades);
 * }
 *}
 */
public final class TurtleToolBuilder {
    private final ResourceKey<ITurtleUpgrade> id;
    private final Item item;
    private Component adjective;
    private float damageMultiplier = TurtleToolSpec.DEFAULT_DAMAGE_MULTIPLIER;
    private @Nullable TagKey<Block> breakable;
    private boolean allowEnchantments = false;
    private TurtleToolDurability consumeDurability = TurtleToolDurability.NEVER;

    private TurtleToolBuilder(ResourceKey<ITurtleUpgrade> id, Item item) {
        this.id = id;
        adjective = Component.translatable(UpgradeBase.getDefaultAdjective(id.location()));
        this.item = item;
    }

    public static TurtleToolBuilder tool(ResourceLocation id, Item item) {
        return new TurtleToolBuilder(ITurtleUpgrade.createKey(id), item);
    }

    public static TurtleToolBuilder tool(ResourceKey<ITurtleUpgrade> id, Item item) {
        return new TurtleToolBuilder(id, item);
    }

    /**
     * Get the id for this turtle tool.
     *
     * @return The upgrade id.
     */
    public ResourceKey<ITurtleUpgrade> id() {
        return id;
    }

    /**
     * Specify a custom adjective for this tool. By default this takes its adjective from the upgrade id.
     *
     * @param adjective The new adjective to use.
     * @return The tool builder, for further use.
     */
    public TurtleToolBuilder adjective(Component adjective) {
        this.adjective = adjective;
        return this;
    }

    /**
     * The amount of damage a swing of this tool will do. This is multiplied by {@link Attributes#ATTACK_DAMAGE} to
     * get the final damage.
     *
     * @param damageMultiplier The damage multiplier.
     * @return The tool builder, for further use.
     */
    public TurtleToolBuilder damageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
        return this;
    }

    /**
     * Indicate that this upgrade allows items which have been {@linkplain ItemStack#isEnchanted() enchanted} or have
     * {@linkplain DataComponents#ATTRIBUTE_MODIFIERS custom attribute modifiers}.
     *
     * @return The tool builder, for further use.
     */
    public TurtleToolBuilder allowEnchantments() {
        allowEnchantments = true;
        return this;
    }

    /**
     * Set when the tool will consume durability.
     *
     * @param durability The durability predicate.
     * @return The tool builder, for further use.
     */
    public TurtleToolBuilder consumeDurability(TurtleToolDurability durability) {
        consumeDurability = durability;
        return this;
    }

    /**
     * Provide a list of breakable blocks. If not given, the tool can break all blocks. If given, only blocks
     * in this tag, those in {@link ComputerCraftTags.Blocks#TURTLE_ALWAYS_BREAKABLE} and "insta-mine" ones can
     * be broken.
     *
     * @param breakable The tag containing all blocks breakable by this item.
     * @return The tool builder, for further use.
     * @see ComputerCraftTags.Blocks
     */
    public TurtleToolBuilder breakable(TagKey<Block> breakable) {
        this.breakable = breakable;
        return this;
    }

    /**
     * Build the turtle tool upgrade.
     *
     * @return The constructed upgrade.
     */
    public ITurtleUpgrade build() {
        return ComputerCraftAPIService.get().createTurtleTool(new TurtleToolSpec(
            adjective,
            item,
            damageMultiplier,
            allowEnchantments,
            consumeDurability,
            Optional.ofNullable(breakable)
        ));
    }

    /**
     * Build this upgrade and register it for datagen.
     *
     * @param upgrades The registry this upgrade should be added to.
     */
    public void register(BootstrapContext<ITurtleUpgrade> upgrades) {
        upgrades.register(id(), build());
    }
}
