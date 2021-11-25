/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

public class GuiDiskDrive extends ContainerScreen<ContainerDiskDrive>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/disk_drive.png" );

    public GuiDiskDrive( ContainerDiskDrive container, PlayerInventory player, ITextComponent title )
    {
        super( container, player, title );
    }

    @Override
    protected void renderBg( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bind( BACKGROUND );
        blit( transform, leftPos, topPos, 0, 0, imageWidth, imageHeight );
    }

    @Override
    public void render( @Nonnull MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( transform );
        super.render( transform, mouseX, mouseY, partialTicks );
        renderTooltip( transform, mouseX, mouseY );
    }
}
