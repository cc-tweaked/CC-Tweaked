// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.model.turtle.ModelTransformer;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.List;

public class TurtleBlockEntityRenderer implements BlockEntityRenderer<TurtleBlockEntity> {
    private static final ModelResourceLocation NORMAL_TURTLE_MODEL = new ModelResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_normal", "inventory");
    private static final ModelResourceLocation ADVANCED_TURTLE_MODEL = new ModelResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_advanced", "inventory");
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_colour");
    private static final ResourceLocation ELF_OVERLAY_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_elf_overlay");

    private final RandomSource random = RandomSource.create(0);

    private final BlockEntityRenderDispatcher renderer;
    private final Font font;

    public TurtleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        renderer = context.getBlockEntityRenderDispatcher();
        font = context.getFont();
    }

    public static ResourceLocation getTurtleModel(ComputerFamily family, boolean coloured) {
        return switch (family) {
            default -> coloured ? COLOUR_TURTLE_MODEL : NORMAL_TURTLE_MODEL;
            case ADVANCED -> coloured ? COLOUR_TURTLE_MODEL : ADVANCED_TURTLE_MODEL;
        };
    }

    public static @Nullable ResourceLocation getTurtleOverlayModel(@Nullable ResourceLocation overlay, boolean christmas) {
        if (overlay != null) return overlay;
        if (christmas) return ELF_OVERLAY_MODEL;
        return null;
    }

    @Override
    public void render(TurtleBlockEntity turtle, float partialTicks, PoseStack transform, MultiBufferSource buffers, int lightmapCoord, int overlayLight) {
        transform.pushPose();

        // Translate the turtle first, so the label moves with it.
        var offset = turtle.getRenderOffset(partialTicks);
        transform.translate(offset.x, offset.y, offset.z);

        // Render the label
        var label = turtle.getLabel();
        var hit = renderer.cameraHitResult;
        if (label != null && hit.getType() == HitResult.Type.BLOCK && turtle.getBlockPos().equals(((BlockHitResult) hit).getBlockPos())) {
            var mc = Minecraft.getInstance();
            var font = this.font;

            transform.pushPose();
            transform.translate(0.5, 1.2, 0.5);
            transform.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            transform.scale(-0.025f, -0.025f, 0.025f);

            var matrix = transform.last().pose();
            var opacity = (int) (mc.options.getBackgroundOpacity(0.25f) * 255) << 24;
            var width = -font.width(label) / 2.0f;
            // TODO: Check this looks okay
            font.drawInBatch(label, width, (float) 0, 0x20ffffff, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, opacity, lightmapCoord);
            font.drawInBatch(label, width, (float) 0, 0xffffffff, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, lightmapCoord);

            transform.popPose();
        }

        // Then apply rotation and flip if needed.
        transform.translate(0.5f, 0.5f, 0.5f);
        var yaw = turtle.getRenderYaw(partialTicks);
        transform.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        if (label != null && (label.equals("Dinnerbone") || label.equals("Grumm"))) {
            transform.scale(1.0f, -1.0f, 1.0f);
        }
        transform.translate(-0.5f, -0.5f, -0.5f);

        // Render the turtle
        var colour = turtle.getColour();
        var family = turtle.getFamily();
        var overlay = turtle.getOverlay();

        var buffer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        renderModel(transform, buffer, lightmapCoord, overlayLight, getTurtleModel(family, colour != -1), colour == -1 ? null : new int[]{ colour });

        // Render the overlay
        var overlayModel = getTurtleOverlayModel(overlay, Holiday.getCurrent() == Holiday.CHRISTMAS);
        if (overlayModel != null) {
            renderModel(transform, buffer, lightmapCoord, overlayLight, overlayModel, null);
        }

        // Render the upgrades
        renderUpgrade(transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.LEFT, partialTicks);
        renderUpgrade(transform, buffer, lightmapCoord, overlayLight, turtle, TurtleSide.RIGHT, partialTicks);

        transform.popPose();
    }

    private void renderUpgrade(PoseStack transform, VertexConsumer renderer, int lightmapCoord, int overlayLight, TurtleBlockEntity turtle, TurtleSide side, float f) {
        var upgrade = turtle.getUpgrade(side);
        if (upgrade == null) return;
        transform.pushPose();

        var toolAngle = turtle.getToolRenderAngle(side, f);
        transform.translate(0.0f, 0.5f, 0.5f);
        transform.mulPose(Axis.XN.rotationDegrees(toolAngle));
        transform.translate(0.0f, -0.5f, -0.5f);

        var model = TurtleUpgradeModellers.getModel(upgrade, turtle.getAccess(), side);
        pushPoseFromTransformation(transform, model.getMatrix());
        renderModel(transform, renderer, lightmapCoord, overlayLight, model.getModel(), null);
        transform.popPose();

        transform.popPose();
    }

    private void renderModel(PoseStack transform, VertexConsumer renderer, int lightmapCoord, int overlayLight, ResourceLocation modelLocation, @Nullable int[] tints) {
        var modelManager = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getModelManager();
        renderModel(transform, renderer, lightmapCoord, overlayLight, ClientPlatformHelper.get().getModel(modelManager, modelLocation), tints);
    }

    /**
     * Render a block model.
     *
     * @param transform     The current matrix stack.
     * @param renderer      The buffer to write to.
     * @param lightmapCoord The current lightmap coordinate.
     * @param overlayLight  The overlay light.
     * @param model         The model to render.
     * @param tints         Tints for the quads, as an array of RGB values.
     * @see net.minecraft.client.renderer.block.ModelBlockRenderer#renderModel
     */
    private void renderModel(PoseStack transform, VertexConsumer renderer, int lightmapCoord, int overlayLight, BakedModel model, @Nullable int[] tints) {
        for (var facing : DirectionUtil.FACINGS) {
            random.setSeed(42);
            renderQuads(transform, renderer, lightmapCoord, overlayLight, model.getQuads(null, facing, random), tints);
        }

        random.setSeed(42);
        renderQuads(transform, renderer, lightmapCoord, overlayLight, model.getQuads(null, null, random), tints);
    }

    private static void renderQuads(PoseStack transform, VertexConsumer buffer, int lightmapCoord, int overlayLight, List<BakedQuad> quads, @Nullable int[] tints) {
        var matrix = transform.last();
        var inverted = matrix.pose().determinant() < 0;

        for (var bakedquad : quads) {
            var tint = -1;
            if (tints != null && bakedquad.isTinted()) {
                var idx = bakedquad.getTintIndex();
                if (idx >= 0 && idx < tints.length) tint = tints[bakedquad.getTintIndex()];
            }

            var r = (float) (tint >> 16 & 255) / 255.0F;
            var g = (float) (tint >> 8 & 255) / 255.0F;
            var b = (float) (tint & 255) / 255.0F;
            if (inverted) {
                putBulkQuadInvert(buffer, matrix, bakedquad, r, g, b, lightmapCoord, overlayLight);
            } else {
                buffer.putBulkData(matrix, bakedquad, r, g, b, lightmapCoord, overlayLight);
            }
        }
    }

    /**
     * A version of {@link VertexConsumer#putBulkData(PoseStack.Pose, BakedQuad, float, float, float, int, int)} for
     * when the matrix is inverted.
     *
     * @param buffer        The buffer to draw to.
     * @param pose          The current matrix stack.
     * @param quad          The quad to draw.
     * @param red           The red tint of this quad.
     * @param green         The  green tint of this quad.
     * @param blue          The blue tint of this quad.
     * @param lightmapCoord The lightmap coordinate
     * @param overlayLight  The overlay light.
     */
    private static void putBulkQuadInvert(VertexConsumer buffer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, int lightmapCoord, int overlayLight) {
        var matrix = pose.pose();
        // It's a little dubious to transform using this matrix rather than the normal matrix. This mirrors the logic in
        // Direction.rotate (so not out of nowhere!), but is a little suspicious.
        var dirNormal = quad.getDirection().getNormal();
        var normal = matrix.transform(new Vector4f(dirNormal.getX(), dirNormal.getY(), dirNormal.getZ(), 0.0f)).normalize();

        var vertices = quad.getVertices();
        for (var vertex : ModelTransformer.INVERSE_ORDER) {
            var i = vertex * ModelTransformer.STRIDE;

            var x = Float.intBitsToFloat(vertices[i]);
            var y = Float.intBitsToFloat(vertices[i + 1]);
            var z = Float.intBitsToFloat(vertices[i + 2]);
            var transformed = matrix.transform(new Vector4f(x, y, z, 1));

            var u = Float.intBitsToFloat(vertices[i + 4]);
            var v = Float.intBitsToFloat(vertices[i + 5]);
            buffer.vertex(
                transformed.x(), transformed.y(), transformed.z(),
                red, green, blue, 1.0F, u, v, overlayLight, lightmapCoord,
                normal.x(), normal.y(), normal.z()
            );
        }
    }

    private static void pushPoseFromTransformation(PoseStack stack, Transformation transformation) {
        stack.pushPose();

        var trans = transformation.getTranslation();
        stack.translate(trans.x(), trans.y(), trans.z());

        stack.mulPose(transformation.getLeftRotation());

        var scale = transformation.getScale();
        stack.scale(scale.x(), scale.y(), scale.z());

        stack.mulPose(transformation.getRightRotation());
    }
}
