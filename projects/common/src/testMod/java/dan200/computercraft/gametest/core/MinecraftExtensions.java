/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest.core;

import net.minecraft.client.Minecraft;

/**
 * Extensions to {@link Minecraft}, injected via mixin.
 */
public interface MinecraftExtensions {
    boolean computercraft$isRenderingStable();
}
