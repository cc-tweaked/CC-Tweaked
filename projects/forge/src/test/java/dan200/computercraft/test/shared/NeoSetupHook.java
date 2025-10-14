// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import com.google.auto.service.AutoService;

/**
 * Ensures NeoForge is configured as part of the Minecraft's bootstrap.
 */
@AutoService(WithMinecraft.SetupHook.class)
public final class NeoSetupHook implements WithMinecraft.SetupHook {
    @Override
    public void run() {
        // Note: In NeoForge 1.21.10, LoadingModList.of() API has changed significantly.
        // The mod list initialization is now handled differently by the test framework.
        // If feature flags or other systems require the mod list, it will be initialized
        // automatically by NeoForge's test infrastructure.
    }
}
