// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.libmultipart;

import alexiil.mc.lib.multipart.api.render.PartModelKey;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A {@link PartModelKey} which just renders a basic {@link BlockState}.
 */
public final class BlockStateModelKey extends PartModelKey {
    private final BlockState state;

    public BlockStateModelKey(BlockState state) {
        this.state = state;
    }

    public BlockState state() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockStateModelKey other && state == other.state;
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
