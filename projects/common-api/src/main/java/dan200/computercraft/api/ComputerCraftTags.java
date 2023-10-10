// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tags provided by ComputerCraft.
 */
public class ComputerCraftTags {
    public static class Items {
        public static final TagKey<Item> COMPUTER = make("computer");
        public static final TagKey<Item> TURTLE = make("turtle");
        public static final TagKey<Item> WIRED_MODEM = make("wired_modem");
        public static final TagKey<Item> MONITOR = make("monitor");

        /**
         * Items which can be {@linkplain Item#use(Level, Player, InteractionHand) used} when calling
         * {@code turtle.place()}.
         * <p>
         * This does not cover items who handle placing inside {@link Item#useOn(UseOnContext)}, as that is always
         * called.
         */
        public static final TagKey<Item> TURTLE_CAN_PLACE = make("turtle_can_place");

        private static TagKey<Item> make(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(ComputerCraftAPI.MOD_ID, name));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> COMPUTER = make("computer");
        public static final TagKey<Block> TURTLE = make("turtle");
        public static final TagKey<Block> WIRED_MODEM = make("wired_modem");
        public static final TagKey<Block> MONITOR = make("monitor");

        /**
         * Blocks which should be ignored by a {@code peripheral_hub} peripheral.
         * <p>
         * This should include blocks which themselves expose a peripheral hub (such as {@linkplain #WIRED_MODEM wired
         * modems}).
         */
        public static final TagKey<Block> PERIPHERAL_HUB_IGNORE = make("peripheral_hub_ignore");

        /**
         * Blocks which can be broken by any turtle tool.
         */
        public static final TagKey<Block> TURTLE_ALWAYS_BREAKABLE = make("turtle_always_breakable");

        /**
         * Blocks which can be broken by the default shovel tool.
         */
        public static final TagKey<Block> TURTLE_SHOVEL_BREAKABLE = make("turtle_shovel_harvestable");

        /**
         * Blocks which can be broken with the default sword tool.
         */
        public static final TagKey<Block> TURTLE_SWORD_BREAKABLE = make("turtle_sword_harvestable");

        /**
         * Blocks which can be broken with the default hoe tool.
         */
        public static final TagKey<Block> TURTLE_HOE_BREAKABLE = make("turtle_hoe_harvestable");

        /**
         * Block which can be {@linkplain BlockState#use(Level, Player, InteractionHand, BlockHitResult) used} when
         * calling {@code turtle.place()}.
         */
        public static final TagKey<Block> TURTLE_CAN_USE = make("turtle_can_use");

        private static TagKey<Block> make(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(ComputerCraftAPI.MOD_ID, name));
        }
    }
}
