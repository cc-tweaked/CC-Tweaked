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
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import net.minecraft.client.gui.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.StringTextComponent;
import net.minecraft.util.Identifier;

public class GuiComputer<T extends Container> extends ContainerScreen<T>
{
    private static final Identifier BACKGROUND_NORMAL = new Identifier( "computercraft", "textures/gui/corners_normal.png" );
    private static final Identifier BACKGROUND_ADVANCED = new Identifier( "computercraft", "textures/gui/corners_advanced.png" );
    private static final Identifier BACKGROUND_COMMAND = new Identifier( "computercraft", "textures/gui/corners_command.png" );

    private final ComputerFamily m_family;
    private final ClientComputer m_computer;
    private final int m_termWidth;
    private final int m_termHeight;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;


    public GuiComputer( T container, PlayerInventory player, ComputerFamily family, ClientComputer computer, int termWidth, int termHeight )
    {
        super( container, player, new StringTextComponent( "" ) );

        m_family = family;
        m_computer = computer;
        m_termWidth = termWidth;
        m_termHeight = termHeight;
        terminal = null;
    }

    public static GuiComputer<ContainerComputer> create( int id, TileComputer computer, PlayerInventory player )
    {
        return new GuiComputer<>(
            new ContainerComputer( id, computer ), player,
            computer.getFamily(),
            computer.createClientComputer(),
            ComputerCraft.terminalWidth_computer,
            ComputerCraft.terminalHeight_computer
        );
    }

    @Override
    protected void onInitialized()
    {
        client.keyboard.enableRepeatEvents( true );

        int termPxWidth = m_termWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = m_termHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        width = termPxWidth + 4 + 24;
        height = termPxHeight + 4 + 24;

        super.onInitialized();

        terminal = new WidgetTerminal( client, () -> m_computer, m_termWidth, m_termHeight, 2, 2, 2, 2 );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 12 + left, 2 + 12 + top, termPxWidth, termPxHeight );

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

    @Override
    public void drawBackground( float partialTicks, int mouseX, int mouseY )
    {
        // Work out where to draw
        int startX = terminalWrapper.getX() - 2;
        int startY = terminalWrapper.getY() - 2;
        int endX = startX + terminalWrapper.getWidth() + 4;
        int endY = startY + terminalWrapper.getHeight() + 4;

        // Draw terminal
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw a border around the terminal
        GlStateManager.color4f( 1.0f, 1.0f, 1.0f, 1.0f );
        switch( m_family )
        {
            case Normal:
            default:
            {
                client.getTextureManager().bindTexture( BACKGROUND_NORMAL );
                break;
            }
            case Advanced:
            {
                client.getTextureManager().bindTexture( BACKGROUND_ADVANCED );
                break;
            }
            case Command:
            {
                client.getTextureManager().bindTexture( BACKGROUND_COMMAND );
                break;
            }
        }

        drawTexturedRect( startX - 12, startY - 12, 12, 28, 12, 12 );
        drawTexturedRect( startX - 12, endY, 12, 40, 12, 16 );
        drawTexturedRect( endX, startY - 12, 24, 28, 12, 12 );
        drawTexturedRect( endX, endY, 24, 40, 12, 16 );

        drawTexturedRect( startX, startY - 12, 0, 0, endX - startX, 12 );
        drawTexturedRect( startX, endY, 0, 12, endX - startX, 16 );

        drawTexturedRect( startX - 12, startY, 0, 28, 12, endY - startY );
        drawTexturedRect( endX, startY, 36, 28, 12, endY - startY );
    }

    @Override
    public void draw( int mouseX, int mouseY, float partialTicks )
    {
        drawBackground( 0 );
        super.draw( mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( mouseX, mouseY );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }

    @Override
    public boolean mouseReleased( double x, double y, int button )
    {
        return (getFocused() != null && getFocused().mouseReleased( x, y, button ))
            || super.mouseReleased( x, y, button );
    }
}
