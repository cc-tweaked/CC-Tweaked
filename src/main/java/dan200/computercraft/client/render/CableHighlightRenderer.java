/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID, value = Dist.CLIENT)
public final class CableHighlightRenderer {
    private CableHighlightRenderer() {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @param event The event to observe
     * @see net.minecraft.client.renderer.LevelRenderer#renderHitOutline
     */
    @SubscribeEvent
    public static void drawHighlight(RenderHighlightEvent.Block event) {
        var hit = event.getTarget();
        var pos = hit.getBlockPos();
        var world = event.getCamera().getEntity().getCommandSenderWorld();
        var info = event.getCamera();

        var state = world.getBlockState(pos);

        // We only care about instances with both cable and modem.
        if (state.getBlock() != Registry.ModBlocks.CABLE.get() || state.getValue(BlockCable.MODEM).getFacing() == null || !state.getValue(BlockCable.CABLE)) {
            return;
        }

        event.setCanceled(true);

        var shape = WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? CableShapes.getModemShape(state)
            : CableShapes.getCableShape(state);

        var cameraPos = info.getPosition();
        var xOffset = pos.getX() - cameraPos.x();
        var yOffset = pos.getY() - cameraPos.y();
        var zOffset = pos.getZ() - cameraPos.z();

        var buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        var matrix4f = event.getPoseStack().last().pose();
        var normal = event.getPoseStack().last().normal();
        // TODO: Can we just accesstransformer out LevelRenderer.renderShape?
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            var xDelta = (float) (x2 - x1);
            var yDelta = (float) (y2 - y1);
            var zDelta = (float) (z2 - z1);
            var len = Mth.sqrt(xDelta * xDelta + yDelta * yDelta + zDelta * zDelta);
            xDelta = xDelta / len;
            yDelta = yDelta / len;
            zDelta = zDelta / len;

            buffer
                .vertex(matrix4f, (float) (x1 + xOffset), (float) (y1 + yOffset), (float) (z1 + zOffset))
                .color(0, 0, 0, 0.4f)
                .normal(normal, xDelta, yDelta, zDelta)
                .endVertex();
            buffer
                .vertex(matrix4f, (float) (x2 + xOffset), (float) (y2 + yOffset), (float) (z2 + zOffset))
                .color(0, 0, 0, 0.4f)
                .normal(normal, xDelta, yDelta, zDelta)
                .endVertex();
        });
    }
}
