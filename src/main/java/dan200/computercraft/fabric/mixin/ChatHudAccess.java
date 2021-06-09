/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

@Mixin (ChatHud.class)
public interface ChatHudAccess {
	@Invoker
	void callAddMessage(Text text, int messageId);

    @Invoker
    void callRemoveMessage(int messageId);
}
