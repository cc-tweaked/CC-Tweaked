// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CableHighlightRenderer {
    private CableHighlightRenderer() {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @param camera The current camera.
     * @param hit    The block hit result for the current player.
     * @return The custom renderer.
     */
    public static BlockOutlineRenderer.@Nullable Renderer drawHighlight(Camera camera, BlockHitResult hit) {
        var player = camera.entity();
        if (player == null) return null;

        var pos = hit.getBlockPos();
        var state = player.level().getBlockState(pos);

        // We only care about instances with both cable and modem.
        if (state.getBlock() != ModRegistry.Blocks.CABLE.get() || state.getValue(CableBlock.MODEM).getFacing() == null || !state.getValue(CableBlock.CABLE)) {
            return null;
        }

        var shape = WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? CableShapes.getModemShape(state)
            : CableShapes.getCableShape(state);

        return (transform, buffer, colour, width) -> {
            var normal = new Vector3f();
            shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                normal.set((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
                buffer.addVertex(transform, (float) x1, (float) y1, (float) z1).setColor(colour).setNormal(transform, normal).setLineWidth(width);
                buffer.addVertex(transform, (float) x2, (float) y2, (float) z2).setColor(colour).setNormal(transform, normal).setLineWidth(width);
            });
        };
    }
}
