/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

import static net.minecraft.util.math.Direction.*;

/**
 * Overrides monitor highlighting to only render the outline of the <em>whole</em> monitor, rather than the current
 * block. This means you do not get an intrusive outline on top of the screen.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class MonitorHighlightRenderer
{
    private MonitorHighlightRenderer()
    {
    }

    @SubscribeEvent
    public static void drawHighlight( DrawHighlightEvent.HighlightBlock event )
    {
        // Preserve normal behaviour when crouching.
        if( event.getInfo().getFocusedEntity().isInSneakingPose() ) return;

        World world = event.getInfo().getFocusedEntity().getEntityWorld();
        BlockPos pos = event.getTarget().getBlockPos();

        BlockEntity tile = world.getBlockEntity( pos );
        if( !(tile instanceof TileMonitor) ) return;

        TileMonitor monitor = (TileMonitor) tile;
        event.setCanceled( true );

        // Determine which sides are part of the external faces of the monitor, and so which need to be rendered.
        EnumSet<Direction> faces = EnumSet.allOf( Direction.class );
        Direction front = monitor.getFront();
        faces.remove( front );
        if( monitor.getXIndex() != 0 ) faces.remove( monitor.getRight().getOpposite() );
        if( monitor.getXIndex() != monitor.getWidth() - 1 ) faces.remove( monitor.getRight() );
        if( monitor.getYIndex() != 0 ) faces.remove( monitor.getDown().getOpposite() );
        if( monitor.getYIndex() != monitor.getHeight() - 1 ) faces.remove( monitor.getDown() );

        MatrixStack transformStack = event.getMatrix();
        Vec3d cameraPos = event.getInfo().getPos();
        transformStack.push();
        transformStack.translate( pos.getX() - cameraPos.getX(), pos.getY() - cameraPos.getY(), pos.getZ() - cameraPos.getZ() );

        // I wish I could think of a better way to do this
        VertexConsumer buffer = event.getBuffers().getBuffer( RenderLayer.getLines() );
        Matrix4f transform = transformStack.peek().getModel();
        if( faces.contains( NORTH ) || faces.contains( WEST ) ) line( buffer, transform, 0, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( WEST ) ) line( buffer, transform, 0, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( EAST ) ) line( buffer, transform, 1, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( EAST ) ) line( buffer, transform, 1, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( DOWN ) ) line( buffer, transform, 0, 0, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( DOWN ) ) line( buffer, transform, 0, 0, 1, EAST );
        if( faces.contains( NORTH ) || faces.contains( UP ) ) line( buffer, transform, 0, 1, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( UP ) ) line( buffer, transform, 0, 1, 1, EAST );
        if( faces.contains( WEST ) || faces.contains( DOWN ) ) line( buffer, transform, 0, 0, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( DOWN ) ) line( buffer, transform, 1, 0, 0, SOUTH );
        if( faces.contains( WEST ) || faces.contains( UP ) ) line( buffer, transform, 0, 1, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( UP ) ) line( buffer, transform, 1, 1, 0, SOUTH );

        transformStack.pop();
    }

    private static void line( VertexConsumer buffer, Matrix4f transform, float x, float y, float z, Direction direction )
    {
        buffer.vertex( transform, x, y, z ).color( 0, 0, 0, 0.4f ).next();
        buffer.vertex( transform,
            x + direction.getOffsetX(),
            y + direction.getOffsetY(),
            z + direction.getOffsetZ()
        ).color( 0, 0, 0, 0.4f ).next();
    }
}
