// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.shared.lectern.CustomLecternBlockEntity;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.LecternBlock;

/**
 * A block entity renderer for our {@linkplain CustomLecternBlockEntity lectern}.
 * <p>
 * This largely follows {@link LecternRenderer}, but with support for multiple types of item.
 */
public class CustomLecternRenderer implements BlockEntityRenderer<CustomLecternBlockEntity> {
    private final LecternPrintoutModel printoutModel;

    public CustomLecternRenderer(BlockEntityRendererProvider.Context context) {
        printoutModel = new LecternPrintoutModel();
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
        }

        poseStack.popPose();
    }
}
