/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GuiDiskDrive extends HandledScreen<ContainerDiskDrive> {
    private static final Identifier BACKGROUND = new Identifier("computercraft", "textures/gui/disk_drive.png");

    public GuiDiskDrive(ContainerDiskDrive container, PlayerInventory player, Text title) {
        super(container, player, title);
    }

    @Override
    public void render(@Nonnull MatrixStack transform, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(transform);
        super.render(transform, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(transform, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(@Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.client.getTextureManager()
                   .bindTexture(BACKGROUND);
        this.drawTexture(transform, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}
