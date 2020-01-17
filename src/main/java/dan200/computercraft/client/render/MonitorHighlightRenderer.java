/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;

import static net.minecraft.util.EnumFacing.*;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Side.CLIENT )
public final class MonitorHighlightRenderer
{
    private static final float EXPAND = 0.002f;

    private MonitorHighlightRenderer()
    {
    }

    @SubscribeEvent
    public static void drawHighlight( DrawBlockHighlightEvent event )
    {
        if( event.getTarget().typeOfHit != RayTraceResult.Type.BLOCK || event.getPlayer().isSneaking() ) return;

        World world = event.getPlayer().getEntityWorld();
        BlockPos pos = event.getTarget().getBlockPos();

        if( world.getBlockState( pos ).getBlock() != ComputerCraft.Blocks.peripheral ) return;

        TileEntity tile = world.getTileEntity( pos );
        if( !(tile instanceof TileMonitor) ) return;

        TileMonitor monitor = (TileMonitor) tile;
        event.setCanceled( true );

        // Determine which sides are part of the external faces of the monitor, and so which need to be rendered.
        EnumSet<EnumFacing> faces = EnumSet.allOf( EnumFacing.class );
        EnumFacing front = monitor.getFront();
        faces.remove( front );
        if( monitor.getXIndex() != 0 ) faces.remove( monitor.getRight().getOpposite() );
        if( monitor.getXIndex() != monitor.getWidth() - 1 ) faces.remove( monitor.getRight() );
        if( monitor.getYIndex() != 0 ) faces.remove( monitor.getDown().getOpposite() );
        if( monitor.getYIndex() != monitor.getHeight() - 1 ) faces.remove( monitor.getDown() );

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GL11.glLineWidth( 2.0F );
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask( false );
        GlStateManager.pushMatrix();

        EntityPlayer player = event.getPlayer();
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.translate( -x + pos.getX(), -y + pos.getY(), -z + pos.getZ() );

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
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void line( BufferBuilder buffer, int x, int y, int z, EnumFacing direction )
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
