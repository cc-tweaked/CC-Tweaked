/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

public class GuiPrinter extends ContainerScreen<ContainerPrinter>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/printer.png" );

    public GuiPrinter( ContainerPrinter container, PlayerInventory player, ITextComponent title )
    {
        super( container, player, title );
    }

    @Override
    protected void renderBg( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bind( BACKGROUND );
        blit( transform, leftPos, topPos, 0, 0, imageWidth, imageHeight );

        if( getMenu().isPrinting() ) blit( transform, leftPos + 34, topPos + 21, 176, 0, 25, 45 );
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        renderTooltip( stack, mouseX, mouseY );
    }
}
