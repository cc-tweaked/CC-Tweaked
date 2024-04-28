// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.impl.upgrades.TurtleToolSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A data provider to generate turtle upgrades.
 * <p>
 * This should be subclassed and registered to a {@link DataGenerator.PackGenerator}. Override the
 * {@link #addUpgrades(Consumer)} function, construct each upgrade, and pass them off to the provided consumer to
 * generate them.
 *
 * @see ITurtleUpgrade
 */
public abstract class TurtleUpgradeDataProvider extends UpgradeDataProvider<ITurtleUpgrade> {
    public TurtleUpgradeDataProvider(PackOutput output) {
        super(output, "Turtle Upgrades", RegistryHelper.TURTLE_UPGRADE, ComputerCraftAPIService.get().turtleUpgradeCodec());
    }

    /**
     * Create a new turtle tool upgrade, such as a pickaxe or shovel.
     *
     * @param id   The ID of this tool.
     * @param item The item used for tool actions. Note, this doesn't inherit all properties of the tool, you may need
     *             to specify {@link ToolBuilder#damageMultiplier(float)} and {@link ToolBuilder#breakable(TagKey)}.
     * @return A tool builder,
     */
    public final ToolBuilder tool(ResourceLocation id, Item item) {
        return new ToolBuilder(id, item);
    }

    /**
     * A builder for custom turtle tool upgrades.
     *
     * @see #tool(ResourceLocation, Item)
     */
    public final class ToolBuilder {
        private final ResourceLocation id;
        private final Item toolItem;
        private String adjective;
        private @Nullable Item craftingItem;
        private float damageMultiplier = TurtleToolSpec.DEFAULT_DAMAGE_MULTIPLIER;
        private @Nullable TagKey<Block> breakable;
        private boolean allowEnchantments = false;
        private TurtleToolDurability consumeDurability = TurtleToolDurability.NEVER;

        ToolBuilder(ResourceLocation id, Item toolItem) {
            this.id = id;
            adjective = UpgradeBase.getDefaultAdjective(id);
            this.toolItem = toolItem;
            craftingItem = null;
        }

        /**
         * Specify a custom adjective for this tool. By default this takes its adjective from the tool item.
         *
         * @param adjective The new adjective to use.
         * @return The tool builder, for further use.
         */
        public ToolBuilder adjective(String adjective) {
            this.adjective = adjective;
            return this;
        }

        /**
         * Specify a custom item which is used to craft this upgrade. By default this is the same as the provided tool
         * item, but you may wish to override it.
         *
         * @param craftingItem The item used to craft this upgrade.
         * @return The tool builder, for further use.
         */
        public ToolBuilder craftingItem(Item craftingItem) {
            this.craftingItem = craftingItem;
            return this;
        }

        /**
         * The amount of damage a swing of this tool will do. This is multiplied by {@link Attributes#ATTACK_DAMAGE} to
         * get the final damage.
         *
         * @param damageMultiplier The damage multiplier.
         * @return The tool builder, for further use.
         */
        public ToolBuilder damageMultiplier(float damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
            return this;
        }

        /**
         * Indicate that this upgrade allows items which have been {@linkplain ItemStack#isEnchanted() enchanted} or have
         * {@linkplain DataComponents#ATTRIBUTE_MODIFIERS custom attribute modifiers}.
         *
         * @return The tool builder, for further use.
         */
        public ToolBuilder allowEnchantments() {
            allowEnchantments = true;
            return this;
        }

        /**
         * Set when the tool will consume durability.
         *
         * @param durability The durability predicate.
         * @return The tool builder, for further use.
         */
        public ToolBuilder consumeDurability(TurtleToolDurability durability) {
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
        public ToolBuilder breakable(TagKey<Block> breakable) {
            this.breakable = breakable;
            return this;
        }

        /**
         * Register this as an upgrade.
         *
         * @param add The callback given to {@link #addUpgrades(Consumer)}.
         */
        public void add(Consumer<Upgrade<ITurtleUpgrade>> add) {
            upgrade(id, ComputerCraftAPIService.get().createTurtleTool(new TurtleToolSpec(
                adjective,
                Optional.ofNullable(craftingItem),
                toolItem,
                damageMultiplier,
                allowEnchantments,
                consumeDurability,
                Optional.ofNullable(breakable)
            ))).add(add);
        }
    }
}
