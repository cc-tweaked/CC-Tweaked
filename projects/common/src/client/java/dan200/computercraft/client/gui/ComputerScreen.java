// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

public final class ComputerScreen<T extends AbstractComputerMenu> extends AbstractComputerScreen<T> {
    public ComputerScreen(T container, Inventory player, Component title) {
        super(container, player, title, BORDER);

        imageWidth = TerminalWidget.getWidth(terminalData.getWidth()) + BORDER * 2 + AbstractComputerMenu.SIDEBAR_WIDTH;
        imageHeight = TerminalWidget.getHeight(terminalData.getHeight()) + BORDER * 2;
    }

    @Override
    protected TerminalWidget createTerminal() {
        return new TerminalWidget(terminalData, input, leftPos + AbstractComputerMenu.SIDEBAR_WIDTH + BORDER, topPos + BORDER);
    }

    @Override
    public void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
        // Draw a border around the terminal
        var terminal = getTerminal();
        ComputerBorderRenderer.render(
            stack.last().pose(), ComputerBorderRenderer.getTexture(family), terminal.getX(), terminal.getY(), getBlitOffset(),
            FULL_BRIGHT_LIGHTMAP, terminal.getWidth(), terminal.getHeight()
        );
        ComputerSidebar.renderBackground(stack, leftPos, topPos + sidebarYOffset);
    }
}
