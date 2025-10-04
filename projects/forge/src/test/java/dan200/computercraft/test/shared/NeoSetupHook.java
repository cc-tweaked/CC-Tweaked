// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;
import net.neoforged.fml.loading.LoadingModList;

import java.util.List;
import java.util.Map;

/**
 * Ensures NeoForge is configured as part of the Minecraft's bootstrap.
 */
@AutoService(WithMinecraft.SetupHook.class)
public final class NeoSetupHook implements WithMinecraft.SetupHook {
    @Override
    public void run() {
        // Feature flags require the loaded mod list to be available, so populate it with some empty data.
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    }
}
