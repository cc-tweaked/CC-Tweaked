/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.gametest;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SharedConstants.class)
class SharedConstantsMixin {
    /**
     * @author SquidDev
     * @reason Disable DFU initialisation. This doesn't have any impact on gameplay, and slightly speeds up tests.
     */
    @Overwrite
    public static void enableDataFixerOptimizations() {
    }
}
