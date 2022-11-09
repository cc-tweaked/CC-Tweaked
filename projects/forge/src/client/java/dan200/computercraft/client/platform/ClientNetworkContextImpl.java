/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;

@AutoService(ClientNetworkContext.class)
public class ClientNetworkContextImpl extends AbstractClientNetworkContext {
    @Override
    public void handlePlayRecord(BlockPos pos, @Nullable SoundEvent sound, @Nullable String name) {
        var mc = Minecraft.getInstance();
        mc.levelRenderer.playStreamingMusic(sound, pos, null);
        if (name != null) mc.gui.setNowPlaying(Component.literal(name));
    }
}
