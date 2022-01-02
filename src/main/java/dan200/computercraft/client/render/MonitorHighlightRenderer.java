/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static net.minecraft.core.Direction.*;

/**
 * Overrides monitor highlighting to only render the outline of the <em>whole</em> monitor, rather than the current
 * block. This means you do not get an intrusive outline on top of the screen.
 */
@Environment( EnvType.CLIENT )
public final class MonitorHighlightRenderer
{
    private MonitorHighlightRenderer()
    {
    }

    public static boolean drawHighlight( PoseStack transformStack, VertexConsumer buffer, Entity entity, double d, double e, double f, BlockPos pos, BlockState blockState )
    {
        // Preserve normal behaviour when crouching.
        if( entity.isCrouching() ) return false;

        Level world = entity.getCommandSenderWorld();

        BlockEntity tile = world.getBlockEntity( pos );
        if( !(tile instanceof TileMonitor monitor) ) return false;

        // Determine which sides are part of the external faces of the monitor, and so which need to be rendered.
        EnumSet<Direction> faces = EnumSet.allOf( Direction.class );
        Direction front = monitor.getFront();
        faces.remove( front );
        if( monitor.getXIndex() != 0 ) faces.remove( monitor.getRight().getOpposite() );
        if( monitor.getXIndex() != monitor.getWidth() - 1 ) faces.remove( monitor.getRight() );
        if( monitor.getYIndex() != 0 ) faces.remove( monitor.getDown().getOpposite() );
        if( monitor.getYIndex() != monitor.getHeight() - 1 ) faces.remove( monitor.getDown() );

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        transformStack.pushPose();
        transformStack.translate( pos.getX() - cameraPos.x(), pos.getY() - cameraPos.y(), pos.getZ() - cameraPos.z() );

        // I wish I could think of a better way to do this
        Matrix4f transform = transformStack.last().pose();
        Matrix3f normal = transformStack.last().normal();
        if( faces.contains( NORTH ) || faces.contains( WEST ) ) line( buffer, transform, normal, 0, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( WEST ) ) line( buffer, transform, normal, 0, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( EAST ) ) line( buffer, transform, normal, 1, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( EAST ) ) line( buffer, transform, normal, 1, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( DOWN ) ) line( buffer, transform, normal, 0, 0, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( DOWN ) ) line( buffer, transform, normal, 0, 0, 1, EAST );
        if( faces.contains( NORTH ) || faces.contains( UP ) ) line( buffer, transform, normal, 0, 1, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( UP ) ) line( buffer, transform, normal, 0, 1, 1, EAST );
        if( faces.contains( WEST ) || faces.contains( DOWN ) ) line( buffer, transform, normal, 0, 0, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( DOWN ) ) line( buffer, transform, normal, 1, 0, 0, SOUTH );
        if( faces.contains( WEST ) || faces.contains( UP ) ) line( buffer, transform, normal, 0, 1, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( UP ) ) line( buffer, transform, normal, 1, 1, 0, SOUTH );

        transformStack.popPose();
        return true;
    }

    private static void line( VertexConsumer buffer, Matrix4f transform, Matrix3f normal, float x, float y, float z, Direction direction )
    {
        buffer
            .vertex( transform, x, y, z )
            .color( 0, 0, 0, 0.4f )
            .normal( normal, direction.getStepX(), direction.getStepY(), direction.getStepZ() )
            .endVertex();
        buffer
            .vertex( transform,
                x + direction.getStepX(),
                y + direction.getStepY(),
                z + direction.getStepZ()
            )
            .color( 0, 0, 0, 0.4f )
            .normal( normal, direction.getStepX(), direction.getStepY(), direction.getStepZ() )
            .endVertex();
    }
}
