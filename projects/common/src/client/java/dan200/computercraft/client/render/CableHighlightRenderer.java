// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;

public final class CableHighlightRenderer {
    private CableHighlightRenderer() {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @param transform    The current transformation matrix.
     * @param bufferSource The buffer to draw to.
     * @param camera       The current camera.
     * @param hit          The block hit result for the current player.
     * @return If we rendered a custom outline.
     * @see net.minecraft.client.renderer.LevelRenderer#renderHitOutline
     */
    public static boolean drawHighlight(PoseStack transform, MultiBufferSource bufferSource, Camera camera, BlockHitResult hit) {
        var pos = hit.getBlockPos();
        var world = camera.getEntity().getCommandSenderWorld();

        var state = world.getBlockState(pos);

        // We only care about instances with both cable and modem.
        if (state.getBlock() != ModRegistry.Blocks.CABLE.get() || state.getValue(CableBlock.MODEM).getFacing() == null || !state.getValue(CableBlock.CABLE)) {
            return false;
        }

        var shape = WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? CableShapes.getModemShape(state)
            : CableShapes.getCableShape(state);

        var cameraPos = camera.getPosition();
        var xOffset = pos.getX() - cameraPos.x();
        var yOffset = pos.getY() - cameraPos.y();
        var zOffset = pos.getZ() - cameraPos.z();

        var buffer = bufferSource.getBuffer(RenderType.lines());
        var matrix4f = transform.last().pose();
        var normal = transform.last().normal();
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

        return true;
    }
}
