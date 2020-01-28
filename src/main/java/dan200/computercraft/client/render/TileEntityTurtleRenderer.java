/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.data.EmptyModelData;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class TileEntityTurtleRenderer extends TileEntityRenderer<TileTurtle>
{
    private static final ModelResourceLocation NORMAL_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_normal", "inventory" );
    private static final ModelResourceLocation ADVANCED_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_advanced", "inventory" );
    private static final ModelResourceLocation COLOUR_TURTLE_MODEL = new ModelResourceLocation( "computercraft:turtle_colour", "inventory" );
    private static final ModelResourceLocation ELF_OVERLAY_MODEL = new ModelResourceLocation( "computercraft:turtle_elf_overlay", "inventory" );

    private final Random random = new Random( 0 );

    public TileEntityTurtleRenderer( TileEntityRendererDispatcher renderDispatcher )
    {
        super( renderDispatcher );
    }

    public static ModelResourceLocation getTurtleModel( ComputerFamily family, boolean coloured )
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

    public static ModelResourceLocation getTurtleOverlayModel( ResourceLocation overlay, boolean christmas )
    {
        if( overlay != null ) return new ModelResourceLocation( overlay, "inventory" );
        if( christmas ) return ELF_OVERLAY_MODEL;
        return null;
    }

    @Override
    public void render( @Nonnull TileTurtle turtle, float partialTicks, @Nonnull MatrixStack transform, @Nonnull IRenderTypeBuffer renderer, int lightmapCoord, int overlayLight )
    {
        // Render the label
        String label = turtle.createProxy().getLabel();
        RayTraceResult hit = renderDispatcher.cameraHitResult;
        if( label != null && hit.getType() == RayTraceResult.Type.BLOCK && turtle.getPos().equals( ((BlockRayTraceResult) hit).getPos() ) )
        {
            Minecraft mc = Minecraft.getInstance();
            FontRenderer font = renderDispatcher.fontRenderer;

            transform.push();
            transform.translate( 0.5, 1.2, 0.5 );
            transform.rotate( mc.getRenderManager().getCameraOrientation() );
            transform.scale( -0.025f, -0.025f, 0.025f );

            Matrix4f matrix = transform.getLast().getPositionMatrix();
            int opacity = (int) (mc.gameSettings.getTextBackgroundOpacity( 0.25f ) * 255) << 24;
            float width = -font.getStringWidth( label ) / 2.0f;
            font.renderString( label, width, (float) 0, 0x20ffffff, false, matrix, renderer, true, opacity, lightmapCoord );
            font.renderString( label, width, (float) 0, 0xffffffff, false, matrix, renderer, false, 0, lightmapCoord );

            transform.pop();
        }

        transform.push();

        // Setup the transform.
        Vec3d offset = turtle.getRenderOffset( partialTicks );
        float yaw = turtle.getRenderYaw( partialTicks );
        transform.translate( offset.x, offset.y, offset.z );

        transform.translate( 0.5f, 0.5f, 0.5f );
        transform.rotate( Vector3f.YP.rotationDegrees( 180.0f - yaw ) );
        if( label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" )) )
        {
            // Flip the model
            transform.scale( 1.0f, -1.0f, 1.0f );
        }
        transform.translate( -0.5f, -0.5f, -0.5f );

        // Render the turtle
        int colour = turtle.getColour();
        ComputerFamily family = turtle.getFamily();
        ResourceLocation overlay = turtle.getOverlay();

        IVertexBuilder buffer = renderer.getBuffer( Atlases.getTranslucentCullBlockType() );
        renderModel( transform, buffer, lightmapCoord, overlayLight, getTurtleModel( family, colour != -1 ), colour == -1 ? null : new int[] { colour } );

        // Render the overlay
        ModelResourceLocation overlayModel = getTurtleOverlayModel( overlay, HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS );
        if( overlayModel != null )
        {
            renderModel( transform, buffer, lightmapCoord, overlayLight, overlayModel, null );
        }

        // Render the upgrades
        renderUpgrade( transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.LEFT, partialTicks );
        renderUpgrade( transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.RIGHT, partialTicks );

        transform.pop();
    }

    private void renderUpgrade( @Nonnull MatrixStack transform, @Nonnull IVertexBuilder renderer, int lightmapCoord, int overlayLight, TileTurtle turtle, TurtleSide side, float f )
    {
        ITurtleUpgrade upgrade = turtle.getUpgrade( side );
        if( upgrade == null ) return;
        transform.push();

        float toolAngle = turtle.getToolRenderAngle( side, f );
        transform.translate( 0.0f, 0.5f, 0.5f );
        transform.rotate( Vector3f.XN.rotationDegrees( toolAngle ) );
        transform.translate( 0.0f, -0.5f, -0.5f );

        TransformedModel model = upgrade.getModel( turtle.getAccess(), side );
        model.getMatrix().push( transform );
        renderModel( transform, renderer, lightmapCoord, overlayLight, model.getModel(), null );
        transform.pop();

        transform.pop();
    }

    private void renderModel( @Nonnull MatrixStack transform, @Nonnull IVertexBuilder renderer, int lightmapCoord, int overlayLight, ModelResourceLocation modelLocation, int[] tints )
    {
        ModelManager modelManager = Minecraft.getInstance().getItemRenderer().getItemModelMesher().getModelManager();
        renderModel( transform, renderer, lightmapCoord, overlayLight, modelManager.getModel( modelLocation ), tints );
    }

    private void renderModel( @Nonnull MatrixStack transform, @Nonnull IVertexBuilder renderer, int lightmapCoord, int overlayLight, IBakedModel model, int[] tints )
    {
        random.setSeed( 0 );
        renderQuads( transform, renderer, lightmapCoord, overlayLight, model.getQuads( null, null, random, EmptyModelData.INSTANCE ), tints );
        for( Direction facing : DirectionUtil.FACINGS )
        {
            renderQuads( transform, renderer, lightmapCoord, overlayLight, model.getQuads( null, facing, random, EmptyModelData.INSTANCE ), tints );
        }
    }

    private static void renderQuads( @Nonnull MatrixStack transform, @Nonnull IVertexBuilder buffer, int lightmapCoord, int overlayLight, List<BakedQuad> quads, int[] tints )
    {
        MatrixStack.Entry matrix = transform.getLast();

        for( BakedQuad bakedquad : quads )
        {
            int tint = -1;
            if( tints != null && bakedquad.hasTintIndex() )
            {
                int idx = bakedquad.getTintIndex();
                if( idx >= 0 && idx < tints.length ) tint = tints[bakedquad.getTintIndex()];
            }

            float f = (float) (tint >> 16 & 255) / 255.0F;
            float f1 = (float) (tint >> 8 & 255) / 255.0F;
            float f2 = (float) (tint & 255) / 255.0F;
            buffer.addVertexData( matrix, bakedquad, f, f1, f2, lightmapCoord, overlayLight, true );
        }
    }
}
