// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

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
         * @see <a href="https://github.com/Creators-of-Create/Create/blob/mc1.20.1/dev/src/main/java/com/simibubi/create/content/contraptions/BlockMovementChecks.java">{@code BlockMovementChecks}</a>
         */
        public static final TagKey<Block> CREATE_BRITTLE = make("create", "brittle");

        private static TagKey<Block> make(String mod, String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(mod, name));
        }
    }
}
