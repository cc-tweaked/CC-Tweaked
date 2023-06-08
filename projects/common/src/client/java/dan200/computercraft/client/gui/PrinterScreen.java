// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.shared.peripheral.printer.PrinterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The GUI for printers.
 */
public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("computercraft", "textures/gui/printer.png");

    public PrinterScreen(PrinterMenu container, Inventory player, Component title) {
        super(container, player, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (getMenu().isPrinting()) graphics.blit(BACKGROUND, leftPos + 34, topPos + 21, 176, 0, 25, 45);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
