/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import static net.minecraft.util.math.Direction.DOWN;
import static net.minecraft.util.math.Direction.EAST;
import static net.minecraft.util.math.Direction.NORTH;
import static net.minecraft.util.math.Direction.SOUTH;
import static net.minecraft.util.math.Direction.UP;
import static net.minecraft.util.math.Direction.WEST;

import java.util.EnumSet;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class MonitorHighlightRenderer {
    private static final float EXPAND = 0.002f;

    private MonitorHighlightRenderer() {
    }

    public static boolean drawHighlight(Camera camera, BlockHitResult hit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player.isSneaking()) {
            return false;
        }

        BlockPos pos = hit.getBlockPos();
        World world = mc.world;

        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof TileMonitor)) {
            return false;
        }

        TileMonitor monitor = (TileMonitor) tile;

        // Determine which sides are part of the external faces of the monitor, and so which need to be rendered.
        EnumSet<Direction> faces = EnumSet.allOf(Direction.class);
        Direction front = monitor.getFront();
        faces.remove(front);
        if (monitor.getXIndex() != 0) {
            faces.remove(monitor.getRight()
                                .getOpposite());
        }
        if (monitor.getXIndex() != monitor.getWidth() - 1) {
            faces.remove(monitor.getRight());
        }
        if (monitor.getYIndex() != 0) {
            faces.remove(monitor.getDown()
                                .getOpposite());
        }
        if (monitor.getYIndex() != monitor.getHeight() - 1) {
            faces.remove(monitor.getDown());
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                                         GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                         GlStateManager.SourceFactor.ONE,
                                         GlStateManager.DestFactor.ZERO);
        GlStateManager.lineWidth(Math.max(2.5F, (float) mc.window.getFramebufferWidth() / 1920.0F * 2.5F));
        GlStateManager.disableTexture();
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();

        GlStateManager.translated(pos.getX() - camera.getPos()
                                                     .getX(),
                                  pos.getY() - camera.getPos()
                                                     .getY(),
                                  pos.getZ() - camera.getPos()
                                                     .getZ());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        // I wish I could think of a better way to do this
        if (faces.contains(NORTH) || faces.contains(WEST)) {
            line(buffer, 0, 0, 0, UP);
        }
        if (faces.contains(SOUTH) || faces.contains(WEST)) {
            line(buffer, 0, 0, 1, UP);
        }
        if (faces.contains(NORTH) || faces.contains(EAST)) {
            line(buffer, 1, 0, 0, UP);
        }
        if (faces.contains(SOUTH) || faces.contains(EAST)) {
            line(buffer, 1, 0, 1, UP);
        }
        if (faces.contains(NORTH) || faces.contains(DOWN)) {
            line(buffer, 0, 0, 0, EAST);
        }
        if (faces.contains(SOUTH) || faces.contains(DOWN)) {
            line(buffer, 0, 0, 1, EAST);
        }
        if (faces.contains(NORTH) || faces.contains(UP)) {
            line(buffer, 0, 1, 0, EAST);
        }
        if (faces.contains(SOUTH) || faces.contains(UP)) {
            line(buffer, 0, 1, 1, EAST);
        }
        if (faces.contains(WEST) || faces.contains(DOWN)) {
            line(buffer, 0, 0, 0, SOUTH);
        }
        if (faces.contains(EAST) || faces.contains(DOWN)) {
            line(buffer, 1, 0, 0, SOUTH);
        }
        if (faces.contains(WEST) || faces.contains(UP)) {
            line(buffer, 0, 1, 0, SOUTH);
        }
        if (faces.contains(EAST) || faces.contains(UP)) {
            line(buffer, 1, 1, 0, SOUTH);
        }

        tessellator.draw();

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();

        return true;
    }

    private static void line(BufferBuilder buffer, int x, int y, int z, Direction direction) {
        double minX = x == 0 ? -EXPAND : 1 + EXPAND;
        double minY = y == 0 ? -EXPAND : 1 + EXPAND;
        double minZ = z == 0 ? -EXPAND : 1 + EXPAND;

        buffer.vertex(minX, minY, minZ)
              .color(0, 0, 0, 0.4f)
              .next();
        buffer.vertex(minX + direction.getOffsetX() * (1 + EXPAND * 2),
                      minY + direction.getOffsetY() * (1 + EXPAND * 2),
                      minZ + direction.getOffsetZ() * (1 + EXPAND * 2))
              .color(0, 0, 0, 0.4f)
              .next();
    }
}
