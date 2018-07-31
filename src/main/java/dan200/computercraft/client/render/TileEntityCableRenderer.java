package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.modem.BlockCable;
import dan200.computercraft.shared.peripheral.modem.BlockCableModemVariant;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * Render breaking animation only over part of a {@link TileCable}.
 */
public class TileEntityCableRenderer extends TileEntitySpecialRenderer<TileCable>
{
    @Override
    public void render( @Nonnull TileCable te, double x, double y, double z, float partialTicks, int destroyStage, float alpha )
    {
        if( destroyStage < 0 ) return;

        BlockPos pos = te.getPos();

        Minecraft mc = Minecraft.getMinecraft();

        RayTraceResult hit = mc.objectMouseOver;
        if( hit == null || !hit.getBlockPos().equals( pos ) ) return;

        if( MinecraftForgeClient.getRenderPass() != 0 ) return;

        World world = te.getWorld();
        IBlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        if( block != ComputerCraft.Blocks.cable ) return;

        state = state.getActualState( world, pos );
        if( te.getPeripheralType() != PeripheralType.Cable && WorldUtil.isVecInsideInclusive( ComputerCraft.Blocks.cable.getModemBounds( state ), hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
        {
            state = block.getDefaultState().withProperty( BlockCable.MODEM, state.getValue( BlockCable.MODEM ) );
        }
        else
        {
            state = state.withProperty( BlockCable.MODEM, BlockCableModemVariant.None );
        }

        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState( state );
        if( model == null ) return;

        preRenderDamagedBlocks();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.BLOCK );
        buffer.setTranslation( x - pos.getX(), y - pos.getY(), z - pos.getZ() );
        buffer.noColor();

        ForgeHooksClient.setRenderLayer( block.getRenderLayer() );

        // See BlockRendererDispatcher#renderBlockDamage
        TextureAtlasSprite breakingTexture = mc.getTextureMapBlocks().getAtlasSprite( "minecraft:blocks/destroy_stage_" + destroyStage );
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(
            world,
            ForgeHooksClient.getDamageModel( model, breakingTexture, state, world, pos ),
            state, pos, buffer, true
        );

        ForgeHooksClient.setRenderLayer( BlockRenderLayer.SOLID );

        buffer.setTranslation( 0, 0, 0 );
        Tessellator.getInstance().draw();

        postRenderDamagedBlocks();
    }

    /**
     * @see RenderGlobal#preRenderDamagedBlocks()
     */
    private void preRenderDamagedBlocks()
    {
        GlStateManager.disableLighting();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate( GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO );
        GlStateManager.enableBlend();
        GlStateManager.color( 1.0F, 1.0F, 1.0F, 0.5F );
        GlStateManager.doPolygonOffset( -3.0F, -3.0F );
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc( 516, 0.1F );
        GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();
    }

    /**
     * @see RenderGlobal#postRenderDamagedBlocks()
     */
    private void postRenderDamagedBlocks()
    {
        GlStateManager.disableAlpha();
        GlStateManager.doPolygonOffset( 0.0F, 0.0F );
        GlStateManager.disablePolygonOffset();
        GlStateManager.disablePolygonOffset();
        GlStateManager.depthMask( true );
        GlStateManager.popMatrix();
    }
}
