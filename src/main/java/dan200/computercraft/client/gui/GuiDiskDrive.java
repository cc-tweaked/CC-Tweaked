/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import net.minecraft.client.gui.ContainerScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class GuiDiskDrive extends ContainerScreen<ContainerDiskDrive>
{
    private static final Identifier BACKGROUND = new Identifier( "computercraft", "textures/gui/disk_drive.png" );

    public GuiDiskDrive( ContainerDiskDrive container, PlayerInventory inventory )
    {
        super( container, inventory, ComputerCraft.Blocks.diskDrive.getTextComponent() );
    }

    @Override
    protected void drawForeground( int par1, int par2 )
    {
        String title = name.getFormattedText();
        fontRenderer.draw( title, (width - fontRenderer.getStringWidth( title )) / 2.0f, 6, 0x404040 );
        fontRenderer.draw( I18n.translate( "container.inventory" ), 8, (height - 96) + 2, 0x404040 );
    }

    @Override
    protected void drawBackground( float f, int i, int j )
    {
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        client.getTextureManager().bindTexture( BACKGROUND );
        drawTexturedRect( left, top, 0, 0, width, height );
    }

    @Override
    public void draw( int mouseX, int mouseY, float partialTicks )
    {
        drawBackground();
        super.draw( mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( mouseX, mouseY );
    }
}
