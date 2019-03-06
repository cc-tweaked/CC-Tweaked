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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Render breaking animation only over part of a {@link TileCable}.
 */
public class TileEntityCableRenderer extends BlockEntityRenderer<TileCable>
{
    private static final Random random = new Random();

    @Override
    public void render( @Nonnull TileCable te, double x, double y, double z, float partialTicks, int destroyStage )
    {
        if( destroyStage < 0 ) return;

        BlockPos pos = te.getPos();

        MinecraftClient mc = MinecraftClient.getInstance();

        HitResult hit = mc.hitResult;
        if( !(hit instanceof BlockHitResult) || !((BlockHitResult) hit).getBlockPos().equals( pos ) ) return;

        World world = te.getWorld();
        BlockState state = te.getCachedState();
        Block block = state.getBlock();
        if( block != ComputerCraft.Blocks.cable ) return;

        VoxelShape shape = CableShapes.getModemShape( state );
        state = te.hasModem() && shape.getBoundingBox().contains( hit.getPos().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? block.getDefaultState().with( BlockCable.MODEM, state.get( BlockCable.MODEM ) )
            : state.with( BlockCable.MODEM, CableModemVariant.None );

        BakedModel model = mc.getBlockRenderManager().getModel( state );

        preRenderDamagedBlocks();

        BufferBuilder buffer = Tessellator.getInstance().getBufferBuilder();
        buffer.begin( GL11.GL_QUADS, VertexFormats.POSITION_COLOR_UV_LMAP );
        buffer.setOffset( x - pos.getX(), y - pos.getY(), z - pos.getZ() );
        buffer.disableColor();

        // See BlockRendererDispatcher#renderBlockDamage
        Sprite breakingTexture = mc.getSpriteAtlas().getSprite( DESTROY_STAGE_TEXTURES[destroyStage] );
        mc.getBlockRenderManager().tesselateDamage( state, pos, breakingTexture, world );

        buffer.setOffset( 0, 0, 0 );
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
        GlStateManager.blendFuncSeparate(
            GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
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
