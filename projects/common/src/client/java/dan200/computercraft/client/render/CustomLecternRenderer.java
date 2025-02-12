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
import dan200.computercraft.shared.lectern.CustomLecternBlockEntity;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.LecternBlock;

import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * A block entity renderer for our {@linkplain CustomLecternBlockEntity lectern}.
 * <p>
 * This largely follows {@link LecternRenderer}, but with support for multiple types of item.
 */
public class CustomLecternRenderer implements BlockEntityRenderer<CustomLecternBlockEntity> {
    private final LecternPrintoutModel printoutModel;
    private final LecternPocketModel pocketModel;

    public CustomLecternRenderer(BlockEntityRendererProvider.Context context) {
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
            pocketModel.render(poseStack, buffer, packedLight, packedOverlay, pocket.getFamily(), pocket.getColour(item));

            var computer = ClientPocketComputers.get(item);
            var terminal = computer == null ? null : computer.getTerminal();
            if (terminal != null) renderPocketTerminal(poseStack, buffer, terminal);
        }

        poseStack.popPose();
    }

    private static void renderPocketTerminal(PoseStack poseStack, MultiBufferSource buffer, Terminal terminal) {
        var width = terminal.getWidth() * FONT_WIDTH;
        var height = terminal.getHeight() * FONT_HEIGHT;

        // Scale the terminal down to fit in the available space.
        var scaleX = LecternPocketModel.TERM_WIDTH / (width + MARGIN * 2);
        var scaleY = LecternPocketModel.TERM_HEIGHT / (height + MARGIN * 2);
        var scale = Math.min(scaleX, scaleY);

        // Jiggle the terminal about a bit, so it's centred.
        poseStack.mulPose(Axis.YP.rotationDegrees(90f));
        poseStack.translate(0, 1f / 32.0f, 1 / 16.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.scale(scale, scale, -1.0f);
        poseStack.translate(-0.5f * width, -0.5f * height, 0);

        // Convert the model dimensions to terminal space, then find out how much padding we need.
        var marginX = ((LecternPocketModel.TERM_WIDTH / scale) - width) / 2;
        var marginY = ((LecternPocketModel.TERM_HEIGHT / scale) - height) / 2;

        var quadEmitter = FixedWidthFontRenderer.toVertexConsumer(poseStack, buffer.getBuffer(RenderTypes.TERMINAL));
        FixedWidthFontRenderer.drawTerminal(quadEmitter, 0, 0, terminal, marginY, marginY, marginX, marginX);
    }
}
