// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.shared.peripheral.printer.PrinterMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("computercraft", "textures/gui/printer.png");

    public PrinterScreen(PrinterMenu container, Inventory player, Component title) {
        super(container, player, title);
    }

    @Override
    protected void renderBg(PoseStack transform, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        blit(transform, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (getMenu().isPrinting()) blit(transform, leftPos + 34, topPos + 21, 176, 0, 25, 45);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);
        renderTooltip(stack, mouseX, mouseY);
    }
}
