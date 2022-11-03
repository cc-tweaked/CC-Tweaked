/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

public final class GuiComputer<T extends ContainerComputerBase> extends ComputerScreenBase<T> {
    public GuiComputer(T container, Inventory player, Component title) {
        super(container, player, title, BORDER);

        imageWidth = WidgetTerminal.getWidth(terminalData.getWidth()) + BORDER * 2 + ComputerSidebar.WIDTH;
        imageHeight = WidgetTerminal.getHeight(terminalData.getHeight()) + BORDER * 2;
    }

    @Override
    protected WidgetTerminal createTerminal() {
        return new WidgetTerminal(terminalData, input, leftPos + ComputerSidebar.WIDTH + BORDER, topPos + BORDER);
    }

    @Override
    public void renderBg(@Nonnull PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        // Draw a border around the terminal
        ComputerBorderRenderer.render(
            stack.last().pose(), ComputerBorderRenderer.getTexture(family), terminal.x, terminal.y, getBlitOffset(),
            FULL_BRIGHT_LIGHTMAP, terminal.getWidth(), terminal.getHeight()
        );
        ComputerSidebar.renderBackground(stack, leftPos, topPos + sidebarYOffset);
    }
}
