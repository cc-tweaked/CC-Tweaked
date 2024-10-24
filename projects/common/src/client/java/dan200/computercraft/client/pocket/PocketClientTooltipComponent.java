// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.pocket;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.SpriteRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.pocket.items.PocketTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nullable;
import java.util.UUID;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

/**
 * Renders the pocket computer's terminal in the item's tooltip.
 * <p>
 * The rendered terminal is downscaled by a factor of {@link #SCALE}.
 */
public class PocketClientTooltipComponent implements ClientTooltipComponent {
    private static final float SCALE = 0.5f;

    private final UUID id;
    private final ComputerFamily family;

    public PocketClientTooltipComponent(PocketTooltipComponent component) {
        this.id = component.id();
        this.family = component.family();
    }

    private @Nullable PocketComputerData computer() {
        return ClientPocketComputers.get(id);
    }

    private @Nullable NetworkedTerminal terminal() {
        var computer = computer();
        return computer == null ? null : computer.getTerminal();
    }

    @Override
    public int getHeight() {
        var terminal = terminal();
        if (terminal == null) return 0;

        return (int) Math.ceil(
            (terminal.getHeight() * FixedWidthFontRenderer.FONT_HEIGHT + ComputerBorderRenderer.BORDER * 2 + ComputerBorderRenderer.MARGIN * 2) * SCALE
        );
    }

    @Override
    public int getWidth(Font font) {
        var terminal = terminal();
        if (terminal == null) return 0;

        return (int) Math.ceil(
            (terminal.getWidth() * FixedWidthFontRenderer.FONT_WIDTH + ComputerBorderRenderer.BORDER * 2 + ComputerBorderRenderer.MARGIN * 2) * SCALE
        );
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        var terminal = terminal();
        if (terminal == null) return;

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(SCALE, SCALE, 1);


        render(pose, guiGraphics.bufferSource(), terminal);

        pose.popPose();
    }

    private void render(PoseStack stack, MultiBufferSource buffers, Terminal terminal) {
        var width = terminal.getWidth() * FONT_WIDTH + MARGIN * 2;
        var height = terminal.getHeight() * FONT_HEIGHT + MARGIN * 2;

        var renderer = SpriteRenderer.createForGui(stack.last().pose(), buffers.getBuffer(RenderTypes.GUI_SPRITES));
        ComputerBorderRenderer.render(renderer, GuiSprites.getComputerTextures(family), BORDER, BORDER, width, height, false);

        var quadEmitter = FixedWidthFontRenderer.toVertexConsumer(stack, buffers.getBuffer(RenderTypes.TERMINAL));
        FixedWidthFontRenderer.drawTerminal(quadEmitter, BORDER + MARGIN, BORDER + MARGIN, terminal, MARGIN, MARGIN, MARGIN, MARGIN);
    }
}
