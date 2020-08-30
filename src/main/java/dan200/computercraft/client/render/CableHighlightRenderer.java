/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class CableHighlightRenderer
{
    private CableHighlightRenderer()
    {
    }

    /**
     * Draw an outline for a specific part of a cable "Multipart".
     *
     * @param event The event to observe
     * @see WorldRenderer#drawSelectionBox(MatrixStack, IVertexBuilder, Entity, double, double, double, BlockPos, BlockState)
     */
    @SubscribeEvent
    public static void drawHighlight( DrawHighlightEvent.HighlightBlock event )
    {
        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        World world = event.getInfo().getFocusedEntity().getEntityWorld();
        Camera info = event.getInfo();

        BlockState state = world.getBlockState( pos );

        // We only care about instances with both cable and modem.
        if( state.getBlock() != Registry.ModBlocks.CABLE.get() || state.get( BlockCable.MODEM ).getFacing() == null || !state.get( BlockCable.CABLE ) )
        {
            return;
        }

        event.setCanceled( true );

        VoxelShape shape = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getPos().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? CableShapes.getModemShape( state )
            : CableShapes.getCableShape( state );

        Vec3d cameraPos = info.getPos();
        double xOffset = pos.getX() - cameraPos.getX();
        double yOffset = pos.getY() - cameraPos.getY();
        double zOffset = pos.getZ() - cameraPos.getZ();

        VertexConsumer buffer = event.getBuffers().getBuffer( RenderLayer.getLines() );
        Matrix4f matrix4f = event.getMatrix().peek().getModel();
        shape.forEachEdge( ( x1, y1, z1, x2, y2, z2 ) -> {
            buffer.vertex( matrix4f, (float) (x1 + xOffset), (float) (y1 + yOffset), (float) (z1 + zOffset) )
                .color( 0, 0, 0, 0.4f ).next();
            buffer.vertex( matrix4f, (float) (x2 + xOffset), (float) (y2 + yOffset), (float) (z2 + zOffset) )
                .color( 0, 0, 0, 0.4f ).next();
        } );
    }
}
