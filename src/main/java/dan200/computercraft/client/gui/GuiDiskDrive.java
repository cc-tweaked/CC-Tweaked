/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;

public class GuiDiskDrive extends HandledScreen<ContainerDiskDrive>
{
    private static final Identifier BACKGROUND = new Identifier( "computercraft", "textures/gui/disk_drive.png" );

    public GuiDiskDrive( ContainerDiskDrive container, PlayerInventory player, Text title )
    {
        super( container, player, title );
    }

    @Override
    public void render( @Nonnull MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( transform );
        super.render( transform, mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( transform, mouseX, mouseY );
    }

    @Override
    protected void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        drawTexture( transform, x, y, 0, 0, backgroundWidth, backgroundHeight );
    }
}
