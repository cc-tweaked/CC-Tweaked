// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tags defined by external mods.
 */
public final class ExternalModTags {
    private ExternalModTags() {
    }

    /**
     * Block tags defined by external mods.
     */
    public static final class Blocks {
        private Blocks() {
        }

        /**
         * Create's "brittle" tag, used to determine if this block needs to be moved before its neighbours.
         *
         * @see com.simibubi.create.content.contraptions.BlockMovementChecks#isBrittle(BlockState)
         */
        public static final TagKey<Block> CREATE_BRITTLE = make(CreateIntegration.ID, "brittle");

        private static TagKey<Block> make(String mod, String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(mod, name));
        }
    }
}
