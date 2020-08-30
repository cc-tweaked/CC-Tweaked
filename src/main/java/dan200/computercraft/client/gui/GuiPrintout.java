/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.PrintoutRenderer.*;

public class GuiPrintout extends HandledScreen<ContainerHeldItem>
{
    private final boolean m_book;
    private final int m_pages;
    private final TextBuffer[] m_text;
    private final TextBuffer[] m_colours;
    private int m_page;

    public GuiPrintout( ContainerHeldItem container, PlayerInventory player, Text title )
    {
        super( container, player, title );

        backgroundHeight = Y_SIZE;

        String[] text = ItemPrintout.getText( container.getStack() );
        m_text = new TextBuffer[text.length];
        for( int i = 0; i < m_text.length; i++ ) m_text[i] = new TextBuffer( text[i] );

        String[] colours = ItemPrintout.getColours( container.getStack() );
        m_colours = new TextBuffer[colours.length];
        for( int i = 0; i < m_colours.length; i++ ) m_colours[i] = new TextBuffer( colours[i] );

        m_page = 0;
        m_pages = Math.max( m_text.length / ItemPrintout.LINES_PER_PAGE, 1 );
        m_book = ((ItemPrintout) container.getStack().getItem()).getType() == ItemPrintout.Type.BOOK;
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        if( super.keyPressed( key, scancode, modifiers ) ) return true;

        if( key == GLFW.GLFW_KEY_RIGHT )
        {
            if( m_page < m_pages - 1 ) m_page++;
            return true;
        }

        if( key == GLFW.GLFW_KEY_LEFT )
        {
            if( m_page > 0 ) m_page--;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled( double x, double y, double delta )
    {
        if( super.mouseScrolled( x, y, delta ) ) return true;
        if( delta < 0 )
        {
            // Scroll up goes to the next page
            if( m_page < m_pages - 1 ) m_page++;
            return true;
        }

        if( delta > 0 )
        {
            // Scroll down goes to the previous page
            if( m_page > 0 ) m_page--;
            return true;
        }

        return false;
    }

    @Override
    protected void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        // Draw the printout
        RenderSystem.color4f( 1.0f, 1.0f, 1.0f, 1.0f );
        RenderSystem.enableDepthTest();

        VertexConsumerProvider.Immediate renderer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        Matrix4f matrix = transform.peek().getModel();
        drawBorder( matrix, renderer, x, y, getZOffset(), m_page, m_pages, m_book );
        drawText( matrix, renderer, x + X_TEXT_MARGIN, y + Y_TEXT_MARGIN, ItemPrintout.LINES_PER_PAGE * m_page, m_text, m_colours );
        renderer.draw();
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        // We must take the background further back in order to not overlap with our printed pages.
        setZOffset( getZOffset() - 1 );
        renderBackground( stack );
        setZOffset( getZOffset() + 1 );

        super.render( stack, mouseX, mouseY, partialTicks );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }
}
