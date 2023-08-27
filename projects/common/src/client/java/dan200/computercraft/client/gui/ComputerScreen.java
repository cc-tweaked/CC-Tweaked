// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.SpriteRenderer;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;

/**
 * A GUI for computers which renders the terminal (and border), but with no UI elements.
 * <p>
 * This is used by computers and pocket computers.
 *
 * @param <T> The concrete type of the associated menu.
 */
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
    public void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Draw a border around the terminal
        var terminal = getTerminal();
        var spriteRenderer = SpriteRenderer.createForGui(graphics, RenderTypes.GUI_SPRITES);
        var computerTextures = GuiSprites.getComputerTextures(family);

        ComputerBorderRenderer.render(
            spriteRenderer, computerTextures,
            terminal.getX(), terminal.getY(), terminal.getWidth(), terminal.getHeight(), false
        );
        ComputerSidebar.renderBackground(spriteRenderer, computerTextures, leftPos, topPos + sidebarYOffset);
        graphics.flush(); // Flush to ensure background textures are drawn before foreground.
    }
}
