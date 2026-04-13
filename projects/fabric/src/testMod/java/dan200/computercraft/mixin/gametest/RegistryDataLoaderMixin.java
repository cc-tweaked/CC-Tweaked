// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import dan200.computercraft.gametest.core.TestMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Register game tests into the dynamic registry. This just mirrors Fabric's own hook, but loading our annotations
 * instead.
 */
@Mixin(RegistryDataLoader.class)
class RegistryDataLoaderMixin {
    @Inject(method = "lambda$load$2", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private static void beforeFreeze(
        List<RegistryLoadTask<?>> loadTasks,
        Map<ResourceKey<?>, Exception> loadingErrors,
        Void ignored,
        CallbackInfoReturnable<RegistryAccess.Frozen> cir
    ) {
        TestMod.registerDynamicEntries(loadTasks);
    }
}
