// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.CommonHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
class ServerLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("UnusedMethod")
    private void addEntity(Entity entity, CallbackInfoReturnable<Boolean> cb) {
        if (CommonHooks.onEntitySpawn(entity)) cb.setReturnValue(true);
    }
}
