/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui.widgets;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IComputerContainer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.BACKGROUND;

public class WidgetTerminal extends Widget
{
    private static final float TERMINATE_TIME = 0.5f;

    private final IComputerContainer m_computer;

    private float m_terminateTimer = 0.0f;
    private float m_rebootTimer = 0.0f;
    private float m_shutdownTimer = 0.0f;

    private int m_lastClickButton = -1;
    private int m_lastClickX = -1;
    private int m_lastClickY = -1;

    private boolean m_focus = false;
    private boolean m_allowFocusLoss = true;

    private int m_leftMargin;
    private int m_rightMargin;
    private int m_topMargin;
    private int m_bottomMargin;

    private final ArrayList<Integer> m_keysDown = new ArrayList<>();

    public WidgetTerminal( int x, int y, int termWidth, int termHeight, IComputerContainer computer, int leftMargin, int rightMargin, int topMargin, int bottomMargin )
    {
        super(
            x, y,
            leftMargin + rightMargin + termWidth * FixedWidthFontRenderer.FONT_WIDTH,
            topMargin + bottomMargin + termHeight * FixedWidthFontRenderer.FONT_HEIGHT
        );

        m_computer = computer;

        m_leftMargin = leftMargin;
        m_rightMargin = rightMargin;
        m_topMargin = topMargin;
        m_bottomMargin = bottomMargin;
    }

    public void setAllowFocusLoss( boolean allowFocusLoss )
    {
        m_allowFocusLoss = allowFocusLoss;
        m_focus = m_focus || !allowFocusLoss;
    }

    @Override
    public boolean onKeyTyped( char ch, int key )
    {
        if( m_focus )
        {
            // Ctrl+V for paste
            if( ch == 22 )
            {
                String clipboard = GuiScreen.getClipboardString();
                if( clipboard != null )
                {
                    // Clip to the first occurrence of \r or \n
                    int newLineIndex1 = clipboard.indexOf( '\r' );
                    int newLineIndex2 = clipboard.indexOf( '\n' );
                    if( newLineIndex1 >= 0 && newLineIndex2 >= 0 )
                    {
                        clipboard = clipboard.substring( 0, Math.min( newLineIndex1, newLineIndex2 ) );
                    }
                    else if( newLineIndex1 >= 0 )
                    {
                        clipboard = clipboard.substring( 0, newLineIndex1 );
                    }
                    else if( newLineIndex2 >= 0 )
                    {
                        clipboard = clipboard.substring( 0, newLineIndex2 );
                    }

                    // Filter the string
                    clipboard = ChatAllowedCharacters.filterAllowedCharacters( clipboard );

                    if( !clipboard.isEmpty() )
                    {
                        // Clip to 512 characters
                        if( clipboard.length() > 512 )
                        {
                            clipboard = clipboard.substring( 0, 512 );
                        }

                        // Queue the "paste" event
                        queueEvent( "paste", new Object[] { clipboard } );
                    }
                }
                return true;
            }

            // Regular keys normally
            if( m_terminateTimer <= 0.0f && m_rebootTimer <= 0.0f && m_shutdownTimer <= 0.0f )
            {
                boolean repeat = Keyboard.isRepeatEvent();
                boolean handled = false;
                if( key > 0 )
                {
                    if( !repeat )
                    {
                        m_keysDown.add( key );
                    }

                    // Queue the "key" event
                    IComputer computer = m_computer.getComputer();
                    if( computer != null ) computer.keyDown( key, repeat );
                    handled = true;
                }

                if( (ch >= 32 && ch <= 126) || (ch >= 160 && ch <= 255) ) // printable chars in byte range
                {
                    // Queue the "char" event
                    queueEvent( "char", new Object[] { Character.toString( ch ) } );
                    handled = true;
                }

                return handled;
            }
        }

        return false;
    }

    @Override
    public void mouseClicked( int mouseX, int mouseY, int button )
    {
        if( mouseX >= getXPosition() && mouseX < getXPosition() + getWidth() &&
            mouseY >= getYPosition() && mouseY < getYPosition() + getHeight() )
        {
            if( !m_focus && button == 0 )
            {
                m_focus = true;
            }

            if( m_focus )
            {
                IComputer computer = m_computer.getComputer();
                if( computer != null && computer.isColour() && button >= 0 && button <= 2 )
                {
                    Terminal term = computer.getTerminal();
                    if( term != null )
                    {
                        int charX = (mouseX - (getXPosition() + m_leftMargin)) / FixedWidthFontRenderer.FONT_WIDTH;
                        int charY = (mouseY - (getYPosition() + m_topMargin)) / FixedWidthFontRenderer.FONT_HEIGHT;
                        charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
                        charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

                        computer.mouseClick( button + 1, charX + 1, charY + 1 );

                        m_lastClickButton = button;
                        m_lastClickX = charX;
                        m_lastClickY = charY;
                    }
                }
            }
        }
        else
        {
            if( m_focus && button == 0 && m_allowFocusLoss )
            {
                m_focus = false;
            }
        }
    }

    @Override
    public boolean onKeyboardInput()
    {
        boolean handled = false;
        for( int i = m_keysDown.size() - 1; i >= 0; --i )
        {
            int key = m_keysDown.get( i );
            if( !Keyboard.isKeyDown( key ) )
            {
                m_keysDown.remove( i );
                if( m_focus )
                {
                    // Queue the "key_up" event
                    IComputer computer = m_computer.getComputer();
                    if( computer != null ) computer.keyUp( key );
                    handled = true;
                }
            }
        }

        return handled;
    }

    @Override
    public void handleMouseInput( int mouseX, int mouseY )
    {
        IComputer computer = m_computer.getComputer();
        if( mouseX >= getXPosition() && mouseX < getXPosition() + getWidth() &&
            mouseY >= getYPosition() && mouseY < getYPosition() + getHeight() &&
            computer != null && computer.isColour() )
        {
            Terminal term = computer.getTerminal();
            if( term != null )
            {
                int charX = (mouseX - (getXPosition() + m_leftMargin)) / FixedWidthFontRenderer.FONT_WIDTH;
                int charY = (mouseY - (getYPosition() + m_topMargin)) / FixedWidthFontRenderer.FONT_HEIGHT;
                charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
                charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

                if( m_lastClickButton >= 0 && !Mouse.isButtonDown( m_lastClickButton ) )
                {
                    if( m_focus ) computer.mouseUp( m_lastClickButton + 1, charX + 1, charY + 1 );
                    m_lastClickButton = -1;
                }

                int wheelChange = Mouse.getEventDWheel();
                if( wheelChange == 0 && m_lastClickButton == -1 )
                {
                    return;
                }

                if( m_focus )
                {
                    if( wheelChange < 0 )
                    {
                        computer.mouseScroll( 1, charX + 1, charY + 1 );
                    }
                    else if( wheelChange > 0 )
                    {
                        computer.mouseScroll( -1, charX + 1, charY + 1 );
                    }

                    if( m_lastClickButton >= 0 && (charX != m_lastClickX || charY != m_lastClickY) )
                    {
                        computer.mouseDrag( m_lastClickButton + 1, charX + 1, charY + 1 );
                        m_lastClickX = charX;
                        m_lastClickY = charY;
                    }
                }
            }
        }
    }

    @Override
    public void update()
    {
        // Handle special keys
        if( m_focus && (Keyboard.isKeyDown( 29 ) || Keyboard.isKeyDown( 157 )) )
        {
            // Ctrl+T for terminate
            if( Keyboard.isKeyDown( 20 ) )
            {
                if( m_terminateTimer < TERMINATE_TIME )
                {
                    m_terminateTimer += 0.05f;
                    if( m_terminateTimer >= TERMINATE_TIME ) queueEvent( "terminate" );
                }
            }
            else
            {
                m_terminateTimer = 0.0f;
            }

            // Ctrl+R for reboot
            if( Keyboard.isKeyDown( 19 ) )
            {
                if( m_rebootTimer < TERMINATE_TIME )
                {
                    m_rebootTimer += 0.05f;
                    if( m_rebootTimer >= TERMINATE_TIME )
                    {
                        IComputer computer = m_computer.getComputer();
                        if( computer != null ) computer.reboot();
                    }
                }
            }
            else
            {
                m_rebootTimer = 0.0f;
            }

            // Ctrl+S for shutdown
            if( Keyboard.isKeyDown( 31 ) )
            {
                if( m_shutdownTimer < TERMINATE_TIME )
                {
                    m_shutdownTimer += 0.05f;
                    if( m_shutdownTimer >= TERMINATE_TIME )
                    {
                        IComputer computer = m_computer.getComputer();
                        if( computer != null ) computer.shutdown();
                    }
                }
            }
            else
            {
                m_shutdownTimer = 0.0f;
            }
        }
        else
        {
            m_terminateTimer = 0.0f;
            m_rebootTimer = 0.0f;
            m_shutdownTimer = 0.0f;
        }
    }

    @Override
    public void draw( Minecraft mc, int xOrigin, int yOrigin, int mouseX, int mouseY )
    {
        int startX = xOrigin + getXPosition();
        int startY = yOrigin + getYPosition();

        synchronized( m_computer )
        {
            // Draw the screen contents
            IComputer computer = m_computer.getComputer();
            Terminal terminal = computer != null ? computer.getTerminal() : null;
            if( terminal != null )
            {
                // Draw the terminal
                boolean greyscale = !computer.isColour();

                Palette palette = terminal.getPalette();

                // Get the data from the terminal first
                // Unfortunately we have to keep the lock for the whole of drawing, so the text doesn't change under us.
                FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();
                boolean tblink = m_focus && terminal.getCursorBlink() && FrameInfo.getGlobalCursorBlink();
                int tw = terminal.getWidth();
                int th = terminal.getHeight();
                int tx = terminal.getCursorX();
                int ty = terminal.getCursorY();

                int x = startX + m_leftMargin;
                int y = startY + m_topMargin;

                // Draw margins
                TextBuffer emptyLine = new TextBuffer( ' ', tw );
                if( m_topMargin > 0 )
                {
                    fontRenderer.drawString( emptyLine, x, startY, terminal.getTextColourLine( 0 ), terminal.getBackgroundColourLine( 0 ), m_leftMargin, m_rightMargin, greyscale, palette );
                }
                if( m_bottomMargin > 0 )
                {
                    fontRenderer.drawString( emptyLine, x, startY + 2 * m_bottomMargin + (th - 1) * FixedWidthFontRenderer.FONT_HEIGHT, terminal.getTextColourLine( th - 1 ), terminal.getBackgroundColourLine( th - 1 ), m_leftMargin, m_rightMargin, greyscale, palette );
                }

                // Draw lines
                for( int line = 0; line < th; line++ )
                {
                    TextBuffer text = terminal.getLine( line );
                    TextBuffer colour = terminal.getTextColourLine( line );
                    TextBuffer backgroundColour = terminal.getBackgroundColourLine( line );
                    fontRenderer.drawString( text, x, y, colour, backgroundColour, m_leftMargin, m_rightMargin, greyscale, palette );
                    y += FixedWidthFontRenderer.FONT_HEIGHT;
                }

                if( tblink && tx >= 0 && ty >= 0 && tx < tw && ty < th )
                {
                    TextBuffer cursor = new TextBuffer( '_', 1 );
                    TextBuffer cursorColour = new TextBuffer( "0123456789abcdef".charAt( terminal.getTextColour() ), 1 );

                    fontRenderer.drawString(
                        cursor,
                        x + FixedWidthFontRenderer.FONT_WIDTH * tx,
                        startY + m_topMargin + FixedWidthFontRenderer.FONT_HEIGHT * ty,
                        cursorColour, null,
                        0, 0,
                        greyscale,
                        palette
                    );
                }
            }
            else
            {
                // Draw a black background
                mc.getTextureManager().bindTexture( BACKGROUND );
                Colour black = Colour.Black;
                GlStateManager.color( black.getR(), black.getG(), black.getB(), 1.0f );
                try
                {
                    drawTexturedModalRect( startX, startY, 0, 0, getWidth(), getHeight() );
                }
                finally
                {
                    GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
                }
            }
        }
    }

    private void queueEvent( String event )
    {
        IComputer computer = m_computer.getComputer();
        if( computer != null ) computer.queueEvent( event );
    }

    private void queueEvent( String event, Object[] args )
    {
        IComputer computer = m_computer.getComputer();
        if( computer != null ) computer.queueEvent( event, args );
    }
}
