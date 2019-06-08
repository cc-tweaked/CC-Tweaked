/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Render breaking animation only over part of a {@link TileCable}.
 */
public class TileEntityCableRenderer extends TileEntityRenderer<TileCable>
{
    private static final Random random = new Random();

    @Override
    public void render( @Nonnull TileCable te, double x, double y, double z, float partialTicks, int destroyStage )
    {
        if( destroyStage < 0 ) return;

        BlockPos pos = te.getPos();

        Minecraft mc = Minecraft.getInstance();

        RayTraceResult hit = mc.objectMouseOver;
        if( hit == null || hit.getType() != RayTraceResult.Type.BLOCK || !((BlockRayTraceResult) hit).getPos().equals( pos ) )
        {
            return;
        }

        if( ForgeHooksClient.getWorldRenderPass() != 0 ) return;

        World world = te.getWorld();
        BlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        if( block != ComputerCraft.Blocks.cable ) return;

        state = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getHitVec().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? block.getDefaultState().with( BlockCable.MODEM, state.get( BlockCable.MODEM ) )
            : state.with( BlockCable.MODEM, CableModemVariant.None );

        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState( state );

        preRenderDamagedBlocks();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.BLOCK );
        buffer.setTranslation( x - pos.getX(), y - pos.getY(), z - pos.getZ() );
        buffer.noColor();

        ForgeHooksClient.setRenderLayer( block.getRenderLayer() );

        // See BlockRendererDispatcher#renderBlockDamage
        TextureAtlasSprite breakingTexture = mc.getTextureMap().getSprite( DESTROY_STAGES[destroyStage] );
        mc.getBlockRendererDispatcher().getBlockModelRenderer().renderModel(
            world,
            ForgeHooksClient.getDamageModel( model, breakingTexture, state, world, pos, 0 ),
            state, pos, buffer, true, random, state.getPositionRandom( pos ), EmptyModelData.INSTANCE
        );

        ForgeHooksClient.setRenderLayer( BlockRenderLayer.SOLID );

        buffer.setTranslation( 0, 0, 0 );
        Tessellator.getInstance().draw();

        postRenderDamagedBlocks();
    }

    /**
     * @see WorldRenderer#preRenderDamagedBlocks()
     */
    private void preRenderDamagedBlocks()
    {
        GlStateManager.disableLighting();

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate( GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GlStateManager.enableBlend();
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 0.5F );
        GlStateManager.polygonOffset( -3.0F, -3.0F );
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc( 516, 0.1F );
        GlStateManager.enableAlphaTest();
        GlStateManager.pushMatrix();
    }

    /**
     * @see WorldRenderer#postRenderDamagedBlocks()
     */
    private void postRenderDamagedBlocks()
    {
        GlStateManager.disableAlphaTest();
        GlStateManager.polygonOffset( 0.0F, 0.0F );
        GlStateManager.disablePolygonOffset();
        GlStateManager.disablePolygonOffset();
        GlStateManager.depthMask( true );
        GlStateManager.popMatrix();
    }
}
