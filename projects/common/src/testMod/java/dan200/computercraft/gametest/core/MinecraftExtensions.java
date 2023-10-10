// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import net.minecraft.client.Minecraft;

/**
 * Extensions to {@link Minecraft}, injected via mixin.
 */
public interface MinecraftExtensions {
    boolean computercraft$isRenderingStable();
}
