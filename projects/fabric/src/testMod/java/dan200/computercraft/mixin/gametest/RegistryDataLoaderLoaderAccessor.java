// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/resources/RegistryDataLoader$Loader")
public interface RegistryDataLoaderLoaderAccessor<T> {
    @Accessor("data")
    RegistryDataLoader.RegistryData<T> getData();

    @Accessor("registry")
    WritableRegistry<T> getRegistry();
}
