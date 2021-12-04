/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment( EnvType.CLIENT )
public final class CableHighlightRenderer
{
    private CableHighlightRenderer()
    {
    }

    /*
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @see net.minecraft.client.renderer.LevelRenderer#renderHitOutline
     */
    public static boolean drawHighlight( PoseStack stack, VertexConsumer buffer, Entity entity, double d, double e, double f, BlockPos pos, BlockState state )
    {
        HitResult hitResult = Minecraft.getInstance().hitResult;
        Camera info = Minecraft.getInstance().gameRenderer.getMainCamera();

        // We only care about instances with both cable and modem.
        if( state.getBlock() != Registry.ModBlocks.CABLE || state.getValue( BlockCable.MODEM ).getFacing() == null || !state.getValue( BlockCable.CABLE ) )
        {
            return false;
        }

        Vec3 hitPos = hitResult != null ? hitResult.getLocation() : new Vec3( d, e, f );

        VoxelShape shape = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hitPos.subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? CableShapes.getModemShape( state )
            : CableShapes.getCableShape( state );

        Vec3 cameraPos = info.getPosition();
        double xOffset = pos.getX() - cameraPos.x();
        double yOffset = pos.getY() - cameraPos.y();
        double zOffset = pos.getZ() - cameraPos.z();

        Matrix4f matrix4f = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        shape.forAllEdges( ( x1, y1, z1, x2, y2, z2 ) -> {
            float xDelta = (float) (x2 - x1);
            float yDelta = (float) (y2 - y1);
            float zDelta = (float) (z2 - z1);
            float len = Mth.sqrt( xDelta * xDelta + yDelta * yDelta + zDelta * zDelta );
            xDelta = xDelta / len;
            yDelta = yDelta / len;
            zDelta = zDelta / len;

            buffer
                .vertex( matrix4f, (float) (x1 + xOffset), (float) (y1 + yOffset), (float) (z1 + zOffset) )
                .color( 0, 0, 0, 0.4f )
                .normal( normal, xDelta, yDelta, zDelta )
                .endVertex();
            buffer
                .vertex( matrix4f, (float) (x2 + xOffset), (float) (y2 + yOffset), (float) (z2 + zOffset) )
                .color( 0, 0, 0, 0.4f )
                .normal( normal, xDelta, yDelta, zDelta )
                .endVertex();
        } );

        return true;
    }
}
