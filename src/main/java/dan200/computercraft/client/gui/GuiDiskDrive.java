/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class GuiDiskDrive extends AbstractContainerScreen<ContainerDiskDrive>
{
    private static final Identifier BACKGROUND = new Identifier( "computercraft", "textures/gui/disk_drive.png" );

    public GuiDiskDrive( ContainerDiskDrive container, PlayerInventory inventory )
    {
        super( container, inventory, ComputerCraft.Blocks.diskDrive.getTextComponent() );
    }

    @Override
    protected void drawForeground( int par1, int par2 )
    {
        String title = getTitle().getFormattedText();
        font.draw( title, (containerWidth - font.getStringWidth( title )) / 2.0f, 6, 0x404040 );
        font.draw( I18n.translate( "container.inventory" ), 8, (containerHeight - 96) + 2, 0x404040 );
    }

    @Override
    protected void drawBackground( float partialTicks, int mouseX, int mouseY )
    {
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bindTexture( BACKGROUND );
        blit( left, top, 0, 0, containerWidth, containerHeight );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( mouseX, mouseY );
    }
}
