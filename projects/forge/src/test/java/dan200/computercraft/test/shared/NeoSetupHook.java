// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Ensures NeoForge is configured as part of the Minecraft's bootstrap.
 */
@AutoService(WithMinecraft.SetupHook.class)
public final class NeoSetupHook implements WithMinecraft.SetupHook {
    @Override
    public void run() {
        // Bits of Minecraft depend on the loader being present. Do some nasty things to inject it.
        // TODO: Switch to using NF's JUnit support instead.
        try {
            var ctor = FMLLoader.class.getDeclaredConstructor(ClassLoader.class, String[].class, Dist.class, boolean.class, Path.class);
            ctor.setAccessible(true);
            var loader = ctor.newInstance(null, new String[0], Dist.CLIENT, false, Path.of("."));

            var modListField = FMLLoader.class.getDeclaredField("loadingModList");
            modListField.setAccessible(true);
            modListField.set(loader, LoadingModList.of(List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
