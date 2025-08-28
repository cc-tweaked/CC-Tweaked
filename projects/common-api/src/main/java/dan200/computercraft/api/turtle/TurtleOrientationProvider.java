// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.turtle;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A provider for custom block orientation transformations during turtle placement.
 * <p>
 * This allows mod authors to register custom handling for their blocks when turtles
 * place them with orientation parameters like "left", "up", etc.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerOrientationProvider
 */
@FunctionalInterface
public interface TurtleOrientationProvider {
    /**
     * Transform a block state based on turtle orientation parameters.
     *
     * @param state The original block state that was placed
     * @param params The orientation parameters from the turtle.place call
     * @return The transformed block state, or the original state if no transformation is needed.
     *         Return null to indicate this provider doesn't handle this block.
     */
    @Nullable
    BlockState transform(BlockState state, OrientationParameters params);

    /**
     * Parameters passed to turtle.place for controlling block orientation.
     */
    public static final class OrientationParameters {
        private final @Nullable Direction blockFacing;
        private final @Nullable Boolean isTop;
        private final boolean isUpsideDown;
        private final boolean isGroundAttachment;
        private final @Nullable Direction clickFace;

        public OrientationParameters(@Nullable Direction blockFacing,
                                   @Nullable Boolean isTop,
                                   boolean isUpsideDown,
                                   boolean isGroundAttachment,
                                   @Nullable Direction clickFace) {
            this.blockFacing = blockFacing;
            this.isTop = isTop;
            this.isUpsideDown = isUpsideDown;
            this.isGroundAttachment = isGroundAttachment;
            this.clickFace = clickFace;
        }

        /**
         * The direction the block should face, or null if no facing was specified.
         */
        public @Nullable Direction getBlockFacing() {
            return blockFacing;
        }

        /**
         * Whether the block should be on top (true), bottom (false), or unspecified (null).
         * Primarily used for slabs.
         */
        public @Nullable Boolean getIsTop() {
            return isTop;
        }

        /**
         * Whether the block should be placed upside down (for stairs, etc.).
         */
        public boolean isUpsideDown() {
            return isUpsideDown;
        }

        /**
         * Whether this is a ground attachment (for torches, etc.).
         */
        public boolean isGroundAttachment() {
            return isGroundAttachment;
        }

        /**
         * Which face of the target block to click, or null if not specified.
         */
        public @Nullable Direction getClickFace() {
            return clickFace;
        }
    }
}
