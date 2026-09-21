// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ComputerCraftAPI.MOD_ID, dist = Dist.CLIENT)
public class ComputerCraftClient {
    public ComputerCraftClient(ModContainer container) {
        // Register our config screen. We exclude http.rules, as we don't currently have a config UI for this.
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, screen) -> new ConfigurationScreen(
            modContainer, screen, (ctx, key, element) -> key.equals("rules") ? null : element
        ));
    }
}
