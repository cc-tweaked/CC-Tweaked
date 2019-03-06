/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.gui.widgets.WidgetWrapper;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.gui.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;

public class GuiTurtle extends ContainerScreen<ContainerTurtle>
{
    private static final Identifier BACKGROUND_NORMAL = new Identifier( "computercraft", "textures/gui/turtle_normal.png" );
    private static final Identifier BACKGROUND_ADVANCED = new Identifier( "computercraft", "textures/gui/turtle_advanced.png" );

    private ContainerTurtle m_container;

    private final ComputerFamily m_family;
    private final ClientComputer m_computer;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiTurtle( TileTurtle turtle, ContainerTurtle container, PlayerInventory player )
    {
        super( container, player, turtle.getDisplayName() );

        m_container = container;
        m_family = turtle.getFamily();
        m_computer = turtle.getClientComputer();

        width = 254;
        height = 217;
    }

    @Override
    protected void onInitialized()
    {
        super.onInitialized();
        client.keyboard.enableRepeatEvents( true );

        int termPxWidth = ComputerCraft.terminalWidth_turtle * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.terminalHeight_turtle * FixedWidthFontRenderer.FONT_HEIGHT;

        terminal = new WidgetTerminal(
            client, () -> m_computer,
            ComputerCraft.terminalWidth_turtle,
            ComputerCraft.terminalHeight_turtle,
            2, 2, 2, 2
        );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 8 + left, 2 + 8 + top, termPxWidth, termPxHeight );

        listeners.add( terminalWrapper );
        setFocused( terminalWrapper );
    }

    @Override
    public void onClosed()
    {
        listeners.remove( terminal );
        terminal = null;
        client.keyboard.enableRepeatEvents( false );
    }

    @Override
    public void update()
    {
        super.update();
        terminal.update();
    }

    private void drawSelectionSlot( boolean advanced )
    {

        // Draw selection slot
        int slot = m_container.getSelectedSlot();
        if( slot >= 0 )
        {
            GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = (slot % 4);
            int slotY = (slot / 4);
            client.getTextureManager().bindTexture( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
            drawTexturedRect( left + m_container.m_turtleInvStartX - 2 + slotX * 18, top + m_container.m_playerInvStartY - 2 + slotY * 18, 0, 217, 24, 24 );
        }
    }

    @Override
    protected void drawBackground( float f, int mouseX, int mouseY )
    {
        // Draw term
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw border/inventory
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        boolean advanced = m_family == ComputerFamily.Advanced;
        client.getTextureManager().bindTexture( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
        drawTexturedRect( left, top, 0, 0, width, height );

        drawSelectionSlot( advanced );
    }

    @Override
    public void draw( int mouseX, int mouseY, float partialTicks )
    {
        drawBackground();
        super.draw( mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( mouseX, mouseY );
    }
}
