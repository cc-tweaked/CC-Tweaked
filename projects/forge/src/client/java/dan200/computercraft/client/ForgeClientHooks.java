// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.sound.SpeakerSound;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-specific dispatch for {@link ClientHooks}.
 */
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, value = Dist.CLIENT)
public final class ForgeClientHooks {
    private ForgeClientHooks() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) ClientHooks.onTick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) ClientHooks.onRenderTick();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) ClientHooks.onWorldUnload();
    }


    @SubscribeEvent
    public static void drawHighlight(RenderHighlightEvent.Block event) {
        if (ClientHooks.drawHighlight(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderText(CustomizeGuiOverlayEvent.DebugText event) {
        ClientHooks.addGameDebugInfo(event.getLeft()::add);
        ClientHooks.addBlockDebugInfo(event.getRight()::add);
    }

    @SubscribeEvent
    public static void onRenderInHand(RenderHandEvent event) {
        if (ClientHooks.onRenderHeldItem(
            event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
            event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderInFrame(RenderItemInFrameEvent event) {
        if (ClientHooks.onRenderItemFrame(
            event.getPoseStack(), event.getMultiBufferSource(), event.getItemFrameEntity(), event.getItemStack(), event.getPackedLight()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playStreaming(PlayStreamingSourceEvent event) {
        if (!(event.getSound() instanceof SpeakerSound sound) || sound.getStream() == null) return;
        ClientHooks.onPlayStreaming(event.getEngine(), event.getChannel(), sound.getStream());
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        ClientRegistry.registerClientCommands(event.getDispatcher(), CommandSourceStack::sendFailure);
    }
}
