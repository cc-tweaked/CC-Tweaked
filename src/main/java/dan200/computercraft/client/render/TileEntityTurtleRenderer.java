/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.entity.TurtleVisionCamera;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import java.util.List;

public class TileEntityTurtleRenderer extends TileEntitySpecialRenderer<TileTurtle>
{
    private static ModelResourceLocation NORMAL_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_normal", "inventory" );
    private static ModelResourceLocation ADVANCED_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_advanced", "inventory" );
    private static ModelResourceLocation COLOUR_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_white", "inventory" );
    private static ModelResourceLocation BEGINNER_TURTLE_MODEL = new ModelResourceLocation( "computercraftedu:CC-TurtleJunior", "inventory" );
    private static ModelResourceLocation BEGINNER_TURTLE_COLOUR_MODEL = new ModelResourceLocation( "computercraftedu:turtleJunior_white", "inventory" );
    private static ModelResourceLocation ELF_OVERLAY_MODEL = new ModelResourceLocation( "computercraft:turtle_elf_overlay", "inventory" );

    public TileEntityTurtleRenderer()
    {
    }

    @Override
    public void render( TileTurtle tileEntity, double posX, double posY, double posZ, float f, int i, float f2 )
    {
        if( tileEntity != null )
        {
            // Check the turtle isn't first person
            Entity viewEntity = Minecraft.getMinecraft().getRenderViewEntity();
            if( viewEntity != null && viewEntity instanceof TurtleVisionCamera )
            {
                TurtleVisionCamera camera = (TurtleVisionCamera) viewEntity;
                if( camera.getTurtle() == tileEntity.getAccess() )
                {
                    return;
                }
            }

            // Render the turtle
            renderTurtleAt( tileEntity, posX, posY, posZ, f, i );
        }
    }

    public static ModelResourceLocation getTurtleModel( ComputerFamily family, boolean coloured )
    {
        switch( family )
        {
            case Normal:
            default:
                return coloured ? COLOUR_TURTLE_MODEL : NORMAL_TURTLE_MODEL;
            case Advanced:
                return coloured ? COLOUR_TURTLE_MODEL : ADVANCED_TURTLE_MODEL;
            case Beginners:
                return coloured ? BEGINNER_TURTLE_COLOUR_MODEL : BEGINNER_TURTLE_MODEL;
        }
    }

    public static ModelResourceLocation getTurtleOverlayModel( ComputerFamily family, ResourceLocation overlay, boolean christmas )
    {
        if( overlay != null )
        {
            return new ModelResourceLocation( overlay, "inventory" );
        }
        else if( christmas && family != ComputerFamily.Beginners )
        {
            return ELF_OVERLAY_MODEL;
        }
        else
        {
            return null;
        }
    }

    private void renderTurtleAt( TileTurtle turtle, double posX, double posY, double posZ, float f, int i )
    {
        IBlockState state = turtle.getWorld().getBlockState( turtle.getPos() );
        GlStateManager.pushMatrix();
        try
        {
            // Setup the transform
            Vec3d offset;
            float yaw;
            offset = turtle.getRenderOffset( f );
            yaw = turtle.getRenderYaw( f );
            GlStateManager.translate( posX + offset.x, posY + offset.y, posZ + offset.z );

            // Render the label
            String label = turtle.createProxy().getLabel();
            if( label != null )
            {
                renderLabel( turtle.getAccess().getPosition(), label );
            }

            // Render the turtle
            GlStateManager.translate( 0.5f, 0.5f, 0.5f );
            GlStateManager.rotate( 180.0f - yaw, 0.0f, 1.0f, 0.0f );
            if( label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" )) )
            {
                // Flip the model and swap the cull face as winding order will have changed.
                GlStateManager.scale( 1.0f, -1.0f, 1.0f );
                GlStateManager.cullFace( GlStateManager.CullFace.FRONT );
            }
            GlStateManager.translate( -0.5f, -0.5f, -0.5f );
            // Render the turtle
            int colour;
            ComputerFamily family;
            ResourceLocation overlay;
            colour = turtle.getColour();
            family = turtle.getFamily();
            overlay = turtle.getOverlay();

            renderModel( state, getTurtleModel( family, colour != -1 ), colour == -1 ? null : new int[] { colour } );

            // Render the overlay
            ModelResourceLocation overlayModel = getTurtleOverlayModel(
                family,
                overlay,
                HolidayUtil.getCurrentHoliday() == Holiday.Christmas
            );
            if( overlayModel != null )
            {
                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
                try
                {
                    renderModel( state, overlayModel, null );
                }
                finally
                {
                    GlStateManager.disableBlend();
                    GlStateManager.enableCull();
                }
            }

            // Render the upgrades
            renderUpgrade( state, turtle, TurtleSide.Left, f );
            renderUpgrade( state, turtle, TurtleSide.Right, f );
        }
        finally
        {
            GlStateManager.popMatrix();
            GlStateManager.cullFace( GlStateManager.CullFace.BACK );
        }
    }

    private void renderUpgrade( IBlockState state, TileTurtle turtle, TurtleSide side, float f )
    {
        ITurtleUpgrade upgrade = turtle.getUpgrade( side );
        if( upgrade != null )
        {
            GlStateManager.pushMatrix();
            try
            {
                float toolAngle = turtle.getToolRenderAngle( side, f );
                GlStateManager.translate( 0.0f, 0.5f, 0.5f );
                GlStateManager.rotate( -toolAngle, 1.0f, 0.0f, 0.0f );
                GlStateManager.translate( 0.0f, -0.5f, -0.5f );

                Pair<IBakedModel, Matrix4f> pair  = upgrade.getModel( turtle.getAccess(), side );
                if( pair != null )
                {
                    if( pair.getRight() != null )
                    {
                        ForgeHooksClient.multiplyCurrentGlMatrix( pair.getRight() );
                    }
                    if( pair.getLeft() != null )
                    {
                        renderModel( state, pair.getLeft(), null );
                    }
                }
            }
            finally
            {
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderModel( IBlockState state, ModelResourceLocation modelLocation, int[] tints )
    {
        Minecraft mc = Minecraft.getMinecraft();
        ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
        renderModel( state, modelManager.getModel( modelLocation ), tints );
    }

    private void renderModel( IBlockState state, IBakedModel model, int[] tints )
    {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.getInstance();
        mc.getTextureManager().bindTexture( TextureMap.LOCATION_BLOCKS_TEXTURE );
        renderQuads( tessellator, model.getQuads( state, null, 0 ), tints );
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            renderQuads( tessellator, model.getQuads( state, facing, 0 ), tints );
        }
    }

    private void renderQuads( Tessellator tessellator, List<BakedQuad> quads, int[] tints )
    {
        BufferBuilder buffer = tessellator.getBuffer();
        VertexFormat format = DefaultVertexFormats.ITEM;
        buffer.begin( GL11.GL_QUADS, format );
        for (BakedQuad quad : quads)
        {
            VertexFormat quadFormat = quad.getFormat();
            if( quadFormat != format )
            {
                tessellator.draw();
                format = quadFormat;
                buffer.begin( GL11.GL_QUADS, format );
            }

            int colour = 0xFFFFFFFF;
            if( quad.hasTintIndex() && tints != null )
            {
                int index = quad.getTintIndex();
                if( index >= 0 && index < tints.length ) colour = tints[ index ] | 0xFF000000;
            }

            LightUtil.renderQuadColor( buffer, quad, colour );
        }
        tessellator.draw();
    }

    private void renderLabel( BlockPos position, String label )
    {
        Minecraft mc = Minecraft.getMinecraft();
        RayTraceResult mop = mc.objectMouseOver;
        if( mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK && mop.getBlockPos().equals( position ) )
        {
            RenderManager renderManager = mc.getRenderManager();
            FontRenderer fontrenderer = renderManager.getFontRenderer();
            float scale = 0.016666668F * 1.6f;

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );
            try
            {
                GlStateManager.translate( 0.5f, 1.25f, 0.5f );
                GlStateManager.rotate( -renderManager.playerViewY, 0.0F, 1.0F, 0.0F );
                GlStateManager.rotate( renderManager.playerViewX, 1.0F, 0.0F, 0.0F );
                GlStateManager.scale( -scale, -scale, scale );

                int yOffset = 0;
                int xOffset = fontrenderer.getStringWidth( label ) / 2;

                // Draw background
                GlStateManager.depthMask( false );
                GlStateManager.disableDepth();
                try
                {
                    // Quad
                    GlStateManager.disableTexture2D();
                    try
                    {
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder renderer = tessellator.getBuffer();
                        renderer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
                        renderer.pos( -xOffset - 1, -1 + yOffset, 0.0D ).color( 0.0F, 0.0F, 0.0F, 0.25F ).endVertex();
                        renderer.pos( -xOffset - 1, 8 + yOffset, 0.0D ).color( 0.0F, 0.0F, 0.0F, 0.25F ).endVertex();
                        renderer.pos( xOffset + 1, 8 + yOffset, 0.0D ).color( 0.0F, 0.0F, 0.0F, 0.25F ).endVertex();
                        renderer.pos( xOffset + 1, -1 + yOffset, 0.0D ).color( 0.0F, 0.0F, 0.0F, 0.25F ).endVertex();
                        tessellator.draw();
                    }
                    finally
                    {
                        GlStateManager.enableTexture2D();
                    }

                    // Text
                    fontrenderer.drawString( label, -fontrenderer.getStringWidth( label ) / 2, yOffset, 0x20ffffff );
                }
                finally
                {
                    GlStateManager.enableDepth();
                    GlStateManager.depthMask( true );
                }

                // Draw foreground text
                fontrenderer.drawString( label, -fontrenderer.getStringWidth( label ) / 2, yOffset, -1 );
            }
            finally
            {
                GlStateManager.disableBlend();
                GlStateManager.enableLighting();
                GlStateManager.popMatrix();
            }
        }
    }
}
