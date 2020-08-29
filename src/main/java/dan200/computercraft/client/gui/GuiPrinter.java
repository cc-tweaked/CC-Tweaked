/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class GuiPrinter extends HandledScreen<ContainerPrinter> {
    private static final Identifier BACKGROUND = new Identifier("computercraft", "textures/gui/printer.png");

    public GuiPrinter(ContainerPrinter container, PlayerInventory player) {
        super(container, player, ComputerCraft.Blocks.printer.getName());
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        String title = this.getTitle().asFormattedString();
        font.draw(title, (this.backgroundWidth - font.getWidth(title)) / 2.0f, 6, 0x404040);
        font.draw(I18n.translate("container.inventory"), 8, this.backgroundHeight - 96 + 2, 0x404040);
    }

    @Override
    protected void drawBackground(float f, int i, int j) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager()
                 .bindTextureInner(BACKGROUND);
        blit(this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        if (this.handler.isPrinting()) {
            blit(this.x + 34, this.y + 21, 176, 0, 25, 45);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(mouseX, mouseY);
    }
}
