// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import com.llamalad7.mixinextras.sugar.Local;
import dan200.computercraft.gametest.core.TestMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Register game tests into the dynamic registry. This just mirrors Fabric's own hook, but loading our annotations
 * instead.
 */
@Mixin(RegistryDataLoader.class)
class RegistryDataLoaderMixin {
    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 1)
    )
    @SuppressWarnings("unused")
    private static void beforeFreeze(
        @Coerce Object loadable,
        List<HolderLookup.RegistryLookup<?>> wrappers,
        List<RegistryDataLoader.RegistryData<?>> entries,
        CallbackInfoReturnable<RegistryAccess.Frozen> cir,
        @Local(ordinal = 2) List<RegistryDataLoaderLoaderAccessor<?>> registriesList
    ) {
        TestMod.registerDynamicEntries(registriesList);
    }
}
