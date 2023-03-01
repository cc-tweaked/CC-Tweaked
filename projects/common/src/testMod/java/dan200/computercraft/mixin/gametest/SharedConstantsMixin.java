// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SharedConstants.class)
class SharedConstantsMixin {
    /**
     * Disable DFU initialisation.
     *
     * @author SquidDev
     * @reason This doesn't have any impact on gameplay, and slightly speeds up tests.
     */
    @Overwrite
    public static void enableDataFixerOptimizations() {
    }
}
