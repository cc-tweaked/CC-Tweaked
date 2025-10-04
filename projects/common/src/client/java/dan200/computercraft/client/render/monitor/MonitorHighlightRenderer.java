// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.client.render.BlockOutlineRenderer;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.EnumSet;

import static net.minecraft.core.Direction.*;

/**
 * Overrides monitor highlighting to only render the outline of the <em>whole</em> monitor, rather than the current
 * block. This means you do not get an intrusive outline on top of the screen.
 */
public final class MonitorHighlightRenderer {
    private MonitorHighlightRenderer() {
    }

    public static boolean drawHighlight(PoseStack transformStack, MultiBufferSource bufferSource, Camera camera, BlockHitResult hit) {
        // Preserve normal behaviour when crouching.
        if (camera.getEntity().isCrouching()) return false;

        var world = camera.getEntity().level();
        var pos = hit.getBlockPos();

        if (!(world.getBlockEntity(pos) instanceof MonitorBlockEntity monitor)) return false;

        // Determine which sides are part of the external faces of the monitor, and so which need to be rendered.
        var faces = EnumSet.allOf(Direction.class);
        var front = monitor.getFront();
        faces.remove(front);
        if (monitor.getXIndex() != 0) faces.remove(monitor.getRight().getOpposite());
        if (monitor.getXIndex() != monitor.getWidth() - 1) faces.remove(monitor.getRight());
        if (monitor.getYIndex() != 0) faces.remove(monitor.getDown().getOpposite());
        if (monitor.getYIndex() != monitor.getHeight() - 1) faces.remove(monitor.getDown());

        var cameraPos = camera.getPosition();
        transformStack.pushPose();
        transformStack.translate(pos.getX() - cameraPos.x(), pos.getY() - cameraPos.y(), pos.getZ() - cameraPos.z());

        var transform = transformStack.last();
        BlockOutlineRenderer.render(bufferSource, (buffer, colour) -> draw(buffer, transform, faces, colour));

        transformStack.popPose();
        return true;
    }

    private static void draw(VertexConsumer buffer, PoseStack.Pose transform, EnumSet<Direction> faces, int colour) {
        // I wish I could think of a better way to do this
        if (faces.contains(NORTH) || faces.contains(WEST)) line(buffer, transform, 0, 0, 0, UP, colour);
        if (faces.contains(SOUTH) || faces.contains(WEST)) line(buffer, transform, 0, 0, 1, UP, colour);
        if (faces.contains(NORTH) || faces.contains(EAST)) line(buffer, transform, 1, 0, 0, UP, colour);
        if (faces.contains(SOUTH) || faces.contains(EAST)) line(buffer, transform, 1, 0, 1, UP, colour);
        if (faces.contains(NORTH) || faces.contains(DOWN)) line(buffer, transform, 0, 0, 0, EAST, colour);
        if (faces.contains(SOUTH) || faces.contains(DOWN)) line(buffer, transform, 0, 0, 1, EAST, colour);
        if (faces.contains(NORTH) || faces.contains(UP)) line(buffer, transform, 0, 1, 0, EAST, colour);
        if (faces.contains(SOUTH) || faces.contains(UP)) line(buffer, transform, 0, 1, 1, EAST, colour);
        if (faces.contains(WEST) || faces.contains(DOWN)) line(buffer, transform, 0, 0, 0, SOUTH, colour);
        if (faces.contains(EAST) || faces.contains(DOWN)) line(buffer, transform, 1, 0, 0, SOUTH, colour);
        if (faces.contains(WEST) || faces.contains(UP)) line(buffer, transform, 0, 1, 0, SOUTH, colour);
        if (faces.contains(EAST) || faces.contains(UP)) line(buffer, transform, 1, 1, 0, SOUTH, colour);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose transform, float x, float y, float z, Direction direction, int colour) {
        buffer
            .addVertex(transform, x, y, z)
            .setColor(colour)
            .setNormal(transform, direction.getStepX(), direction.getStepY(), direction.getStepZ());
        buffer
            .addVertex(transform, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())
            .setColor(colour)
            .setNormal(transform, direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
