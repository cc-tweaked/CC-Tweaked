/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

public class TileEntityTurtleRenderer extends BlockEntityRenderer<TileTurtle>
{
    private static final ModelIdentifier NORMAL_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_normal", "inventory" );
    private static final ModelIdentifier ADVANCED_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_advanced", "inventory" );
    private static final ModelIdentifier COLOUR_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_colour", "inventory" );
    private static final ModelIdentifier ELF_OVERLAY_MODEL = new ModelIdentifier( "computercraft:turtle_elf_overlay", "inventory" );

    private static final FloatBuffer matrixBuf = BufferUtils.createFloatBuffer( 16 );

    @Override
    public void render( TileTurtle tileEntity, double posX, double posY, double posZ, float partialTicks, int breaking )
    {
        if( tileEntity != null ) renderTurtleAt( tileEntity, posX, posY, posZ, partialTicks );
    }

    public static ModelIdentifier getTurtleModel( ComputerFamily family, boolean coloured )
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

    public static ModelIdentifier getTurtleOverlayModel( Identifier overlay, boolean christmas )
    {
        if( overlay != null )
        {
            return new ModelIdentifier( overlay, "inventory" );
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
        if( label != null && renderManager.hitResult != null && renderManager.hitResult instanceof BlockHitResult && turtle.getPos().equals( ((BlockHitResult) renderManager.hitResult).getBlockPos() ) )
        {
            disableLightmap( true );
            GameRenderer.renderFloatingText(
                getFontRenderer(), label,
                (float) posX + 0.5F, (float) posY + 1.2F, (float) posZ + 0.5F, 0,
                renderManager.cameraEntity.getYaw(), renderManager.cameraEntity.getPitch(), false
            );
            disableLightmap( false );
        }

        GlStateManager.pushMatrix();
        try
        {
            BlockState state = turtle.getCachedState();
            // Setup the transform
            Vec3d offset = turtle.getRenderOffset( partialTicks );
            float yaw = turtle.getRenderYaw( partialTicks );
            GlStateManager.translated( posX + offset.x, posY + offset.y, posZ + offset.z );
            // Render the turtle
            GlStateManager.translatef( 0.5f, 0.5f, 0.5f );
            GlStateManager.rotatef( 180.0f - yaw, 0.0f, 1.0f, 0.0f );
            if( label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" )) )
            {
                // Flip the model and swap the cull face as winding order will have changed.
                GlStateManager.scalef( 1.0f, -1.0f, 1.0f );
                GlStateManager.cullFace( GlStateManager.FaceSides.FRONT );
            }
            GlStateManager.translatef( -0.5f, -0.5f, -0.5f );
            // Render the turtle
            int colour = turtle.getColour();
            ComputerFamily family = turtle.getFamily();
            Identifier overlay = turtle.getOverlay();

            renderModel( state, getTurtleModel( family, colour != -1 ), colour == -1 ? null : new int[] { colour } );

            // Render the overlay
            ModelIdentifier overlayModel = getTurtleOverlayModel(
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
            GlStateManager.cullFace( GlStateManager.FaceSides.BACK );
        }
    }

    private void renderUpgrade( BlockState state, TileTurtle turtle, TurtleSide side, float f )
    {
        ITurtleUpgrade upgrade = turtle.getUpgrade( side );
        if( upgrade != null )
        {
            GlStateManager.pushMatrix();
            try
            {
                float toolAngle = turtle.getToolRenderAngle( side, f );
                GlStateManager.translatef( 0.0f, 0.5f, 0.5f );
                GlStateManager.rotatef( -toolAngle, 1.0f, 0.0f, 0.0f );
                GlStateManager.translatef( 0.0f, -0.5f, -0.5f );

                Pair<BakedModel, Matrix4f> pair = upgrade.getModel( turtle.getAccess(), side );
                if( pair != null )
                {
                    if( pair.getRight() != null )
                    {
                        ((Buffer) matrixBuf).clear();
                        float[] t = new float[4];
                        for( int i = 0; i < 4; i++ )
                        {
                            pair.getRight().getColumn( i, t );
                            matrixBuf.put( t );
                        }
                        ((Buffer) matrixBuf).flip();

                        GlStateManager.multMatrix( matrixBuf );
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

    private void renderModel( BlockState state, ModelIdentifier modelLocation, int[] tints )
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BakedModelManager modelManager = mc.getItemRenderer().getModels().getModelManager();
        renderModel( state, modelManager.getModel( modelLocation ), tints );
    }

    private void renderModel( BlockState state, BakedModel model, int[] tints )
    {
        Random random = new Random( 0 );
        Tessellator tessellator = Tessellator.getInstance();
        renderManager.textureManager.bindTexture( SpriteAtlasTexture.BLOCK_ATLAS_TEX );
        renderQuads( tessellator, model.getQuads( state, null, random ), tints );
        for( Direction facing : DirectionUtil.FACINGS )
        {
            renderQuads( tessellator, model.getQuads( state, facing, random ), tints );
        }
    }

    private static void renderQuads( Tessellator tessellator, List<BakedQuad> quads, int[] tints )
    {
        BufferBuilder buffer = tessellator.getBufferBuilder();
        VertexFormat format = VertexFormats.POSITION_COLOR_UV_NORMAL;
        buffer.begin( GL11.GL_QUADS, format );
        for( BakedQuad quad : quads )
        {
            int colour = 0xFFFFFFFF;
            if( quad.hasColor() && tints != null )
            {
                int index = quad.getColorIndex();
                if( index >= 0 && index < tints.length ) colour = tints[index] | 0xFF000000;
            }

            buffer.putVertexData( quad.getVertexData() );
            buffer.setQuadColor( colour );
            Vec3i normal = quad.getFace().getVector();
            buffer.postNormal( normal.getX(), normal.getY(), normal.getZ() );
        }
        tessellator.draw();
    }
}
