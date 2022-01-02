/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

public class GuiPrintout extends AbstractContainerScreen<ContainerHeldItem>
{
    private final boolean book;
    private final int pages;
    private final TextBuffer[] text;
    private final TextBuffer[] colours;
    private int page;

    public GuiPrintout( ContainerHeldItem container, Inventory player, Component title )
    {
        super( container, player, title );

        imageHeight = Y_SIZE;

        String[] text = ItemPrintout.getText( container.getStack() );
        this.text = new TextBuffer[text.length];
        for( int i = 0; i < this.text.length; i++ ) this.text[i] = new TextBuffer( text[i] );

        String[] colours = ItemPrintout.getColours( container.getStack() );
        this.colours = new TextBuffer[colours.length];
        for( int i = 0; i < this.colours.length; i++ ) this.colours[i] = new TextBuffer( colours[i] );

        page = 0;
        pages = Math.max( this.text.length / ItemPrintout.LINES_PER_PAGE, 1 );
        book = ((ItemPrintout) container.getStack().getItem()).getType() == ItemPrintout.Type.BOOK;
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        if( super.keyPressed( key, scancode, modifiers ) ) return true;

        if( key == GLFW.GLFW_KEY_RIGHT )
        {
            if( page < pages - 1 ) page++;
            return true;
        }

        if( key == GLFW.GLFW_KEY_LEFT )
        {
            if( page > 0 ) page--;
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
            if( page < pages - 1 ) page++;
            return true;
        }

        if( delta > 0 )
        {
            // Scroll down goes to the previous page
            if( page > 0 ) page--;
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg( @Nonnull PoseStack transform, float partialTicks, int mouseX, int mouseY )
    {
        // Draw the printout
        RenderSystem.setShaderColor( 1.0f, 1.0f, 1.0f, 1.0f );
        RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource renderer = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        Matrix4f matrix = transform.last().pose();
        drawBorder( matrix, renderer, leftPos, topPos, getBlitOffset(), page, pages, book, FULL_BRIGHT_LIGHTMAP );
        drawText( matrix, renderer, leftPos + X_TEXT_MARGIN, topPos + Y_TEXT_MARGIN, ItemPrintout.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, text, colours );
        renderer.endBatch();
    }

    @Override
    public void render( @Nonnull PoseStack stack, int mouseX, int mouseY, float partialTicks )
    {
        // We must take the background further back in order to not overlap with our printed pages.
        setBlitOffset( getBlitOffset() - 1 );
        renderBackground( stack );
        setBlitOffset( getBlitOffset() + 1 );

        super.render( stack, mouseX, mouseY, partialTicks );
    }

    @Override
    protected void renderLabels( @Nonnull PoseStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }
}
