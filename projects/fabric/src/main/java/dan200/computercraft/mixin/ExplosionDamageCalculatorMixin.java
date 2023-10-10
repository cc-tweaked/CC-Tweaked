// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.turtle.blocks.TurtleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExplosionDamageCalculator.class)
class ExplosionDamageCalculatorMixin {
    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({ "UnusedMethod", "deprecation" })
    private void getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        if (block.getBlock() instanceof TurtleBlock turtle) {
            cir.setReturnValue(Optional.of(
                Math.max(turtle.getExplosionResistance(block, level, pos, explosion), fluid.getExplosionResistance())
            ));
        }
    }
}
