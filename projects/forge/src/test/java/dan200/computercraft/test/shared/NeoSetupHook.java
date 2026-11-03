// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;
import net.minecraft.core.HolderSet;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.List;
import java.util.Map;

/**
 * Ensures NeoForge is configured as part of the Minecraft's bootstrap.
 */
@AutoService(WithMinecraft.SetupHook.class)
public final class NeoSetupHook implements WithMinecraft.SetupHook {
    @Override
    public void beforeBootstrap() {
        // Bits of Minecraft depend on the loader being present. Do some nasty things to inject it.
        // TODO: Switch to using NF's JUnit support instead.
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    @Override
    public void afterBootstrap() {
        try {
            // Allow HolderSet.emptyNamed as a component, as tags have not been loaded. Yes, this is horrible.
            CommonHooks.markComponentClassAsValid(HolderSet.class.getClassLoader().loadClass(HolderSet.class.getName() + "$1"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
