// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest.client;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
class WorldOpenFlowsMixin {
    /**
     * Never prompt for backup/experimental options when running tests.
     *
     * @param access     The current menu.
     * @param customised Whether this rule uses legacy customised worldgen options.
     * @param load       The action run to load the world.
     * @param cancel     The action run to abort loading the world.
     * @param ci         Cancel callback.
     */
    @Inject(at = @At("HEAD"), method = "askForBackup", cancellable = true)
    @SuppressWarnings("unused")
    private void askForBackup(LevelStorageSource.LevelStorageAccess access, boolean customised, Runnable load, Runnable cancel, CallbackInfo ci) {
        load.run();
        ci.cancel();
    }
}
