/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;

@Mixin (MinecraftServer.class)
public interface MinecraftServerAccess {
	@Accessor
	ServerResourceManager getServerResourceManager();
}
