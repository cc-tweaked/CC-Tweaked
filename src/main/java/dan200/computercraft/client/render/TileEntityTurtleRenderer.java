/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class TileEntityTurtleRenderer implements BlockEntityRenderer<TileTurtle>
{
    private static final ModelIdentifier NORMAL_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_normal", "inventory" );
    private static final ModelIdentifier ADVANCED_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_advanced", "inventory" );
    private static final ModelIdentifier COLOUR_TURTLE_MODEL = new ModelIdentifier( "computercraft:turtle_colour", "inventory" );
    private static final ModelIdentifier ELF_OVERLAY_MODEL = new ModelIdentifier( "computercraft:turtle_elf_overlay", "inventory" );

    private final Random random = new Random( 0 );

    BlockEntityRenderDispatcher renderer;

    public TileEntityTurtleRenderer( BlockEntityRendererFactory.Context context )
    {
        renderer = context.getRenderDispatcher();
    }

    public static ModelIdentifier getTurtleModel( ComputerFamily family, boolean coloured )
    {
        switch( family )
        {
            case NORMAL:
            default:
                return coloured ? COLOUR_TURTLE_MODEL : NORMAL_TURTLE_MODEL;
            case ADVANCED:
                return coloured ? COLOUR_TURTLE_MODEL : ADVANCED_TURTLE_MODEL;
        }
    }

    public static ModelIdentifier getTurtleOverlayModel( Identifier overlay, boolean christmas )
    {
        if( overlay != null )
        {
            return new ModelIdentifier( overlay, "inventory" );
        }
        if( christmas )
        {
            return ELF_OVERLAY_MODEL;
        }
        return null;
    }

    @Override
    public void render( @Nonnull TileTurtle turtle, float partialTicks, @Nonnull MatrixStack transform, @Nonnull VertexConsumerProvider buffers,
                        int lightmapCoord, int overlayLight )
    {
        // Render the label
        String label = turtle.createProxy()
            .getLabel();
        HitResult hit = renderer.crosshairTarget;
        if( label != null && hit.getType() == HitResult.Type.BLOCK && turtle.getPos()
            .equals( ((BlockHitResult) hit).getBlockPos() ) )
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer font = mc.textRenderer;

            transform.push();
            transform.translate( 0.5, 1.2, 0.5 );
            transform.multiply( mc.getEntityRenderDispatcher()
                .getRotation() );
            transform.scale( -0.025f, -0.025f, 0.025f );

            Matrix4f matrix = transform.peek()
                .getModel();
            int opacity = (int) (mc.options.getTextBackgroundOpacity( 0.25f ) * 255) << 24;
            float width = -font.getWidth( label ) / 2.0f;
            font.draw( label, width, (float) 0, 0x20ffffff, false, matrix, buffers, true, opacity, lightmapCoord );
            font.draw( label, width, (float) 0, 0xffffffff, false, matrix, buffers, false, 0, lightmapCoord );

            transform.pop();
        }

        transform.push();

        // Setup the transform.
        Vec3d offset = turtle.getRenderOffset( partialTicks );
        float yaw = turtle.getRenderYaw( partialTicks );
        transform.translate( offset.x, offset.y, offset.z );

        transform.translate( 0.5f, 0.5f, 0.5f );
        transform.multiply( Vec3f.POSITIVE_Y.getDegreesQuaternion( 180.0f - yaw ) );
        if( label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" )) )
        {
            // Flip the model
            transform.scale( 1.0f, -1.0f, 1.0f );
        }
        transform.translate( -0.5f, -0.5f, -0.5f );

        // Render the turtle
        int colour = turtle.getColour();
        ComputerFamily family = turtle.getFamily();
        Identifier overlay = turtle.getOverlay();

        VertexConsumer buffer = buffers.getBuffer( TexturedRenderLayers.getEntityTranslucentCull() );
        renderModel( transform, buffer, lightmapCoord, overlayLight, getTurtleModel( family, colour != -1 ), colour == -1 ? null : new int[] { colour } );

        // Render the overlay
        ModelIdentifier overlayModel = getTurtleOverlayModel( overlay, HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS );
        if( overlayModel != null )
        {
            renderModel( transform, buffer, lightmapCoord, overlayLight, overlayModel, null );
        }

        // Render the upgrades
        renderUpgrade( transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.LEFT, partialTicks );
        renderUpgrade( transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.RIGHT, partialTicks );

        transform.pop();
    }

    private void renderUpgrade( @Nonnull MatrixStack transform, @Nonnull VertexConsumer renderer, int lightmapCoord, int overlayLight, TileTurtle turtle,
                                      TurtleSide side, float f )
    {
        ITurtleUpgrade upgrade = turtle.getUpgrade( side );
        if( upgrade == null )
        {
            return;
        }
        transform.push();

        float toolAngle = turtle.getToolRenderAngle( side, f );
        transform.translate( 0.0f, 0.5f, 0.5f );
        transform.multiply( Vec3f.NEGATIVE_X.getDegreesQuaternion( toolAngle ) );
        transform.translate( 0.0f, -0.5f, -0.5f );

        TransformedModel model = upgrade.getModel( turtle.getAccess(), side );
        model.push( transform );
        renderModel( transform, renderer, lightmapCoord, overlayLight, model.getModel(), null );
        transform.pop();

        transform.pop();
    }

    private void renderModel( @Nonnull MatrixStack transform, @Nonnull VertexConsumer renderer, int lightmapCoord, int overlayLight,
                                    ModelIdentifier modelLocation, int[] tints )
    {
        BakedModelManager modelManager = MinecraftClient.getInstance()
            .getItemRenderer()
            .getModels()
            .getModelManager();
        renderModel( transform, renderer, lightmapCoord, overlayLight, modelManager.getModel( modelLocation ), tints );
    }

    private void renderModel( @Nonnull MatrixStack transform, @Nonnull VertexConsumer renderer, int lightmapCoord, int overlayLight, BakedModel model,
                                    int[] tints )
    {
        random.setSeed( 0 );
        renderQuads( transform, renderer, lightmapCoord, overlayLight, model.getQuads( null, null, random ), tints );
        for( Direction facing : DirectionUtil.FACINGS )
        {
            renderQuads( transform, renderer, lightmapCoord, overlayLight, model.getQuads( null, facing, random ), tints );
        }
    }

    private static void renderQuads( @Nonnull MatrixStack transform, @Nonnull VertexConsumer buffer, int lightmapCoord, int overlayLight,
                                     List<BakedQuad> quads, int[] tints )
    {
        MatrixStack.Entry matrix = transform.peek();

        for( BakedQuad bakedquad : quads )
        {
            int tint = -1;
            if( tints != null && bakedquad.hasColor() )
            {
                int idx = bakedquad.getColorIndex();
                if( idx >= 0 && idx < tints.length )
                {
                    tint = tints[bakedquad.getColorIndex()];
                }
            }

            float f = (float) (tint >> 16 & 255) / 255.0F;
            float f1 = (float) (tint >> 8 & 255) / 255.0F;
            float f2 = (float) (tint & 255) / 255.0F;
            buffer.quad( matrix,
                bakedquad,
                new float[] { 1.0F, 1.0F, 1.0F, 1.0F },
                f,
                f1,
                f2,
                new int[] { lightmapCoord, lightmapCoord, lightmapCoord, lightmapCoord },
                overlayLight,
                true );
        }
    }
}
