/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;

import static net.minecraft.util.Direction.*;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class MonitorHighlightRenderer
{
    private static final float EXPAND = 0.002f;

    private MonitorHighlightRenderer()
    {
    }

    @SubscribeEvent
    public static void drawHighlight( DrawBlockHighlightEvent event )
    {
        if( event.getTarget().getType() != RayTraceResult.Type.BLOCK || event.getInfo().func_216773_g().isSneaking() )
        {
            return;
        }

        World world = event.getInfo().func_216773_g().getEntityWorld();
        BlockPos pos = ((BlockRayTraceResult) event.getTarget()).getPos();

        TileEntity tile = world.getTileEntity( pos );
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

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GlStateManager.lineWidth( Math.max( 2.5F, (float) Minecraft.getInstance().mainWindow.getFramebufferWidth() / 1920.0F * 2.5F ) );
        GlStateManager.disableTexture();
        GlStateManager.depthMask( false );
        GlStateManager.pushMatrix();

        Vec3d cameraPos = event.getInfo().getProjectedView();
        GlStateManager.translated( pos.getX() - cameraPos.getX(), pos.getY() - cameraPos.getY(), pos.getZ() - cameraPos.getZ() );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR );

        // I wish I could think of a better way to do this
        if( faces.contains( NORTH ) || faces.contains( WEST ) ) line( buffer, 0, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( WEST ) ) line( buffer, 0, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( EAST ) ) line( buffer, 1, 0, 0, UP );
        if( faces.contains( SOUTH ) || faces.contains( EAST ) ) line( buffer, 1, 0, 1, UP );
        if( faces.contains( NORTH ) || faces.contains( DOWN ) ) line( buffer, 0, 0, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( DOWN ) ) line( buffer, 0, 0, 1, EAST );
        if( faces.contains( NORTH ) || faces.contains( UP ) ) line( buffer, 0, 1, 0, EAST );
        if( faces.contains( SOUTH ) || faces.contains( UP ) ) line( buffer, 0, 1, 1, EAST );
        if( faces.contains( WEST ) || faces.contains( DOWN ) ) line( buffer, 0, 0, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( DOWN ) ) line( buffer, 1, 0, 0, SOUTH );
        if( faces.contains( WEST ) || faces.contains( UP ) ) line( buffer, 0, 1, 0, SOUTH );
        if( faces.contains( EAST ) || faces.contains( UP ) ) line( buffer, 1, 1, 0, SOUTH );

        tessellator.draw();

        GlStateManager.popMatrix();
        GlStateManager.depthMask( true );
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
    }

    private static void line( BufferBuilder buffer, int x, int y, int z, Direction direction )
    {
        double minX = x == 0 ? -EXPAND : 1 + EXPAND;
        double minY = y == 0 ? -EXPAND : 1 + EXPAND;
        double minZ = z == 0 ? -EXPAND : 1 + EXPAND;

        buffer.pos( minX, minY, minZ ).color( 0, 0, 0, 0.4f ).endVertex();
        buffer.pos(
            minX + direction.getXOffset() * (1 + EXPAND * 2),
            minY + direction.getYOffset() * (1 + EXPAND * 2),
            minZ + direction.getZOffset() * (1 + EXPAND * 2)
        ).color( 0, 0, 0, 0.4f ).endVertex();
    }
}
