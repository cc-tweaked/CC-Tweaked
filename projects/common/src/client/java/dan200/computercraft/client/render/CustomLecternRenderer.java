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
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.lectern.CustomLecternBlockEntity;
import dan200.computercraft.shared.media.items.PrintoutData;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * A block entity renderer for our {@linkplain CustomLecternBlockEntity lectern}.
 * <p>
 * This largely follows {@link LecternRenderer}, but with support for multiple types of item.
 */
public class CustomLecternRenderer implements BlockEntityRenderer<CustomLecternBlockEntity, CustomLecternRenderer.State> {
    private static final int POCKET_TERMINAL_RENDER_DISTANCE = 32;

    private final LecternPrintoutModel printoutModel;
    private final LecternPocketModel pocketModel;

    public CustomLecternRenderer(BlockEntityRendererProvider.Context context) {
        printoutModel = new LecternPrintoutModel();
        pocketModel = new LecternPocketModel();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CustomLecternBlockEntity lectern, State state, float f, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(lectern, state, f, camera, overlay);

        var item = lectern.getItem();
        if (item.getItem() instanceof PrintoutItem) {
            state.setPrintout(item.is(ModRegistry.Items.PRINTED_BOOK.get()), PrintoutData.getOrEmpty(item).pages());
        } else if (item.getItem() instanceof PocketComputerItem pocket) {
            var computer = ClientPocketComputers.get(item);
            state.setPocket(
                pocket.getFamily(), DyedItemColor.getOrDefault(item, -1),
                computer == null ? -1 : computer.getLightState(),
                computer == null || !Vec3.atCenterOf(lectern.getBlockPos()).closerThan(camera, POCKET_TERMINAL_RENDER_DISTANCE)
                    ? null : computer.getTerminal()
            );
        } else {
            state.setUnknown();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 1.0625f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.blockState.getValue(LecternBlock.FACING).getClockWise().toYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(67.5f));
        poseStack.translate(0, -0.125f, 0);

        if (state.type == Type.PRINTOUT) {
            if (state.isBook) {
                printoutModel.submitBook(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
            } else {
                // TODO: printoutModel.renderPages(poseStack, vertexConsumer, packedLight, packedOverlay, PrintoutData.getOrEmpty(item).pages());
                // Currently disabled - need to implement page rendering for 1.21.10
                return; // Skip rendering for now
            }
        } else if (state.type == Type.POCKET_COMPUTER) {
            // Render the pocket computer model
            pocketModel.submit(
                poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 
                state.pocketFamily, state.pocketColour,
                state.pocketLight == -1 ? 0xFF000000 : (0xFF000000 | state.pocketLight) // Convert to ARGB format
            );

            // Jiggle the terminal about a bit, so (0, 0) is in the top left of the model's terminal hole.
            poseStack.mulPose(Axis.YP.rotationDegrees(90f));
            poseStack.translate(-0.5 * LecternPocketModel.TERM_WIDTH, 0.5 * LecternPocketModel.TERM_HEIGHT + 1f / 32.0f, 1 / 16.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            // Calculate terminal size and scale
            var terminal = state.pocketTerminal;
            if (terminal != null) {
                var width = ((Number) terminal.getWidth()).floatValue() * FONT_WIDTH;
                var height = ((Number) terminal.getHeight()).floatValue() * FONT_HEIGHT;
                var scaleX = LecternPocketModel.TERM_WIDTH / (width + MARGIN * 2);
                var scaleY = LecternPocketModel.TERM_HEIGHT / (height + MARGIN * 2);
                var scale = Math.min(scaleX, scaleY);
                
                poseStack.scale(scale, scale, -1.0f);
                
                // Convert the model dimensions to terminal space, then find out how large the margin should be.
                var marginX = ((LecternPocketModel.TERM_WIDTH / scale) - width) / 2;
                var marginY = ((LecternPocketModel.TERM_HEIGHT / scale) - height) / 2;
                
                // Render the terminal
                collector.submitCustomGeometry(poseStack, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) -> {
                    var quadEmitter = new FixedWidthFontRenderer.QuadEmitter(pose.pose(), buffer);
                    FixedWidthFontRenderer.drawTerminal(quadEmitter, marginX, marginY, terminal, marginY, marginY, marginX, marginX);
                });
            } else {
                // Render a black screen if no terminal is available
                collector.submitCustomGeometry(poseStack, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) -> {
                    var quadEmitter = new FixedWidthFontRenderer.QuadEmitter(pose.pose(), buffer);
                    FixedWidthFontRenderer.drawEmptyTerminal(quadEmitter, 0, 0, LecternPocketModel.TERM_WIDTH, LecternPocketModel.TERM_HEIGHT);
                });
            }
        }

        poseStack.popPose();
    }

    private enum Type {
        PRINTOUT,
        POCKET_COMPUTER,
        UNKNOWN,
    }

    public static final class State extends BlockEntityRenderState {
        private Type type = Type.PRINTOUT;
        private boolean isBook;

        private ComputerFamily pocketFamily = ComputerFamily.NORMAL;
        private int pocketColour;
        private int pocketLight;
        private @Nullable Terminal pocketTerminal; // TODO: Make this immutable

        private State() {
        }

        private void setUnknown() {
            this.type = Type.UNKNOWN;
            this.pocketTerminal = null;
        }

        private void setPrintout(boolean isBook, int pages) {
            this.type = Type.PRINTOUT;
            this.isBook = isBook;
            // pages parameter not currently used but kept for future printout rendering

            this.pocketTerminal = null;
        }

        private void setPocket(ComputerFamily family, int colour, int light, @Nullable Terminal terminal) {
            this.type = Type.POCKET_COMPUTER;
            this.pocketFamily = family;
            this.pocketColour = colour;
            this.pocketLight = light;
            this.pocketTerminal = terminal;
        }
    }
}
