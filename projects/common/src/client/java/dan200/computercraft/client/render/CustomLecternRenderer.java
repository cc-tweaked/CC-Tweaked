// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.client.model.LecternPocketModel;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.lectern.CustomLecternBlockEntity;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.ARGB32;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.Vec3;

import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * A block entity renderer for our {@linkplain CustomLecternBlockEntity lectern}.
 * <p>
 * This largely follows {@link LecternRenderer}, but with support for multiple types of item.
 */
public class CustomLecternRenderer implements BlockEntityRenderer<CustomLecternBlockEntity> {
    private static final int POCKET_TERMINAL_RENDER_DISTANCE = 32;

    private final BlockEntityRenderDispatcher berDispatcher;
    private final LecternPrintoutModel printoutModel;
    private final LecternPocketModel pocketModel;

    public CustomLecternRenderer(BlockEntityRendererProvider.Context context) {
        berDispatcher = context.getBlockEntityRenderDispatcher();

        printoutModel = new LecternPrintoutModel();
        pocketModel = new LecternPocketModel();
    }

    @Override
    public void render(CustomLecternBlockEntity lectern, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 1.0625f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-lectern.getBlockState().getValue(LecternBlock.FACING).getClockWise().toYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(67.5f));
        poseStack.translate(0, -0.125f, 0);

        var item = lectern.getItem();
        if (item.getItem() instanceof PrintoutItem printout) {
            var vertexConsumer = LecternPrintoutModel.MATERIAL.buffer(buffer, RenderType::entitySolid);
            if (printout.getType() == PrintoutItem.Type.BOOK) {
                printoutModel.renderBook(poseStack, vertexConsumer, packedLight, packedOverlay);
            } else {
                printoutModel.renderPages(poseStack, vertexConsumer, packedLight, packedOverlay, PrintoutItem.getPageCount(item));
            }
        } else if (item.getItem() instanceof PocketComputerItem pocket) {
            var computer = ClientPocketComputers.get(item);

            pocketModel.render(
                poseStack, buffer, packedLight, packedOverlay, pocket.getFamily(), pocket.getColour(item),
                ARGB32.opaque(computer == null || computer.getLightState() == -1 ? Colour.BLACK.getHex() : computer.getLightState())
            );

            // Jiggle the terminal about a bit, so (0, 0) is in the top left of the model's terminal hole.
            poseStack.mulPose(Axis.YP.rotationDegrees(90f));
            poseStack.translate(-0.5 * LecternPocketModel.TERM_WIDTH, 0.5 * LecternPocketModel.TERM_HEIGHT + 1f / 32.0f, 1 / 16.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            // Either render the terminal or a black screen, depending on how close we are.
            var terminal = computer == null ? null : computer.getTerminal();
            var quadEmitter = FixedWidthFontRenderer.toVertexConsumer(poseStack, buffer.getBuffer(RenderTypes.TERMINAL));
            if (terminal != null && Vec3.atCenterOf(lectern.getBlockPos()).closerThan(berDispatcher.camera.getPosition(), POCKET_TERMINAL_RENDER_DISTANCE)) {
                renderPocketTerminal(poseStack, quadEmitter, terminal);
            } else {
                FixedWidthFontRenderer.drawEmptyTerminal(quadEmitter, 0, 0, LecternPocketModel.TERM_WIDTH, LecternPocketModel.TERM_HEIGHT);
            }
        }

        poseStack.popPose();
    }

    private static void renderPocketTerminal(PoseStack poseStack, FixedWidthFontRenderer.QuadEmitter quadEmitter, Terminal terminal) {
        var width = terminal.getWidth() * FONT_WIDTH;
        var height = terminal.getHeight() * FONT_HEIGHT;

        // Scale the terminal down to fit in the available space.
        var scaleX = LecternPocketModel.TERM_WIDTH / (width + MARGIN * 2);
        var scaleY = LecternPocketModel.TERM_HEIGHT / (height + MARGIN * 2);
        var scale = Math.min(scaleX, scaleY);
        poseStack.scale(scale, scale, -1.0f);

        // Convert the model dimensions to terminal space, then find out how large the margin should be.
        var marginX = ((LecternPocketModel.TERM_WIDTH / scale) - width) / 2;
        var marginY = ((LecternPocketModel.TERM_HEIGHT / scale) - height) / 2;

        FixedWidthFontRenderer.drawTerminal(quadEmitter, marginX, marginY, terminal, marginY, marginY, marginX, marginX);
    }
}
