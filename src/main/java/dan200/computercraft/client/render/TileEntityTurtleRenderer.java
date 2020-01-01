/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import java.util.List;

public class TileEntityTurtleRenderer extends TileEntitySpecialRenderer<TileTurtle>
{
    private static final ModelResourceLocation NORMAL_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle", "inventory" );
    private static final ModelResourceLocation ADVANCED_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_advanced", "inventory" );
    private static final ModelResourceLocation COLOUR_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_white", "inventory" );
    private static final ModelResourceLocation ELF_OVERLAY_MODEL = new ModelResourceLocation( "computercraft:turtle_elf_overlay", "inventory" );

    @Override
    public void render( TileTurtle tileEntity, double posX, double posY, double posZ, float partialTicks, int breaking, float f2 )
    {
        if( tileEntity != null ) renderTurtleAt( tileEntity, posX, posY, posZ, partialTicks );
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
        }
    }

    public static ModelResourceLocation getTurtleOverlayModel( ResourceLocation overlay, boolean christmas )
    {
        if( overlay != null )
        {
            return new ModelResourceLocation( overlay, "inventory" );
        }
        else if( christmas )
        {
            return ELF_OVERLAY_MODEL;
        }
        else
        {
            return null;
        }
    }

    private void renderTurtleAt( TileTurtle turtle, double posX, double posY, double posZ, float partialTicks )
    {
        // Render the label
        String label = turtle.createProxy().getLabel();
        if( label != null && rendererDispatcher.cameraHitResult != null && turtle.getPos().equals( rendererDispatcher.cameraHitResult.getBlockPos() ) )
        {
            setLightmapDisabled( true );
            EntityRenderer.drawNameplate(
                getFontRenderer(), label,
                (float) posX + 0.5F, (float) posY + 1.2F, (float) posZ + 0.5F, 0,
                rendererDispatcher.entityYaw, rendererDispatcher.entityPitch, false, false
            );
            setLightmapDisabled( false );
        }

        GlStateManager.pushMatrix();
        try
        {
            IBlockState state = turtle.getWorld().getBlockState( turtle.getPos() );
            // Setup the transform
            Vec3d offset = turtle.getRenderOffset( partialTicks );
            float yaw = turtle.getRenderYaw( partialTicks );
            GlStateManager.translate( posX + offset.x, posY + offset.y, posZ + offset.z );

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
            int colour = turtle.getColour();
            ComputerFamily family = turtle.getFamily();
            ResourceLocation overlay = turtle.getOverlay();

            renderModel( state, getTurtleModel( family, colour != -1 ), colour == -1 ? null : new int[] { colour } );

            // Render the overlay
            ModelResourceLocation overlayModel = getTurtleOverlayModel(
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
            renderUpgrade( state, turtle, TurtleSide.Left, partialTicks );
            renderUpgrade( state, turtle, TurtleSide.Right, partialTicks );
        }
        finally
        {
            GlStateManager.popMatrix();
            GlStateManager.cullFace( GlStateManager.CullFace.BACK );
        }
    }

    private static void renderUpgrade( IBlockState state, TileTurtle turtle, TurtleSide side, float f )
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

                Pair<IBakedModel, Matrix4f> pair = upgrade.getModel( turtle.getAccess(), side );
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

    private static void renderModel( IBlockState state, ModelResourceLocation modelLocation, int[] tints )
    {
        Minecraft mc = Minecraft.getMinecraft();
        ModelManager modelManager = mc.getRenderItem().getItemModelMesher().getModelManager();
        renderModel( state, modelManager.getModel( modelLocation ), tints );
    }

    private static void renderModel( IBlockState state, IBakedModel model, int[] tints )
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

    private static void renderQuads( Tessellator tessellator, List<BakedQuad> quads, int[] tints )
    {
        BufferBuilder buffer = tessellator.getBuffer();
        VertexFormat format = DefaultVertexFormats.ITEM;
        buffer.begin( GL11.GL_QUADS, format );
        for( BakedQuad quad : quads )
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
                if( index >= 0 && index < tints.length ) colour = tints[index] | 0xFF000000;
            }

            LightUtil.renderQuadColor( buffer, quad, colour );
        }
        tessellator.draw();
    }
}
