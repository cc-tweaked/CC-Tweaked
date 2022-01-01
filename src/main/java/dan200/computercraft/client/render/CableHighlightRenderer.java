/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawSelectionEvent;
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
     * @see net.minecraft.client.renderer.LevelRenderer#renderHitOutline
     */
    @SubscribeEvent
    public static void drawHighlight( DrawSelectionEvent.HighlightBlock event )
    {
        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        Level world = event.getCamera().getEntity().getCommandSenderWorld();
        Camera info = event.getCamera();

        BlockState state = world.getBlockState( pos );

        // We only care about instances with both cable and modem.
        if( state.getBlock() != Registry.ModBlocks.CABLE.get() || state.getValue( BlockCable.MODEM ).getFacing() == null || !state.getValue( BlockCable.CABLE ) )
        {
            return;
        }

        event.setCanceled( true );

        VoxelShape shape = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getLocation().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? CableShapes.getModemShape( state )
            : CableShapes.getCableShape( state );

        Vec3 cameraPos = info.getPosition();
        double xOffset = pos.getX() - cameraPos.x();
        double yOffset = pos.getY() - cameraPos.y();
        double zOffset = pos.getZ() - cameraPos.z();

        VertexConsumer buffer = event.getMultiBufferSource().getBuffer( RenderType.lines() );
        Matrix4f matrix4f = event.getPoseStack().last().pose();
        Matrix3f normal = event.getPoseStack().last().normal();
        // TODO: Can we just accesstransformer out LevelRenderer.renderShape?
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
    }
}
