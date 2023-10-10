// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
