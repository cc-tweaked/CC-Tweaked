/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IComputerContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

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

    private int m_lastMouseX = -1;
    private int m_lastMouseY = -1;

    private boolean m_focus = false;
    private boolean m_allowFocusLoss = true;

    private final int leftMargin;
    private final int rightMargin;
    private final int topMargin;
    private final int bottomMargin;

    private final ArrayList<Integer> m_keysDown = new ArrayList<>();

    public WidgetTerminal( int x, int y, int termWidth, int termHeight, IComputerContainer computer, int leftMargin, int rightMargin, int topMargin, int bottomMargin )
    {
        super(
            x, y,
            leftMargin + rightMargin + termWidth * FixedWidthFontRenderer.FONT_WIDTH,
            topMargin + bottomMargin + termHeight * FixedWidthFontRenderer.FONT_HEIGHT
        );

        m_computer = computer;

        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
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
                        int charX = (mouseX - (getXPosition() + leftMargin)) / FixedWidthFontRenderer.FONT_WIDTH;
                        int charY = (mouseY - (getYPosition() + topMargin)) / FixedWidthFontRenderer.FONT_HEIGHT;
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
        if ( computer == null || !computer.isColour() ) return;

        if( mouseX >= getXPosition() && mouseX < getXPosition() + getWidth() &&
            mouseY >= getYPosition() && mouseY < getYPosition() + getHeight() )
        {
            Terminal term = computer.getTerminal();
            if( term != null )
            {
                int charX = (mouseX - (getXPosition() + leftMargin)) / FixedWidthFontRenderer.FONT_WIDTH;
                int charY = (mouseY - (getYPosition() + topMargin)) / FixedWidthFontRenderer.FONT_HEIGHT;
                charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
                charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

                if( m_lastClickButton >= 0 && !Mouse.isButtonDown( m_lastClickButton ) )
                {
                    if( m_focus ) computer.mouseUp( m_lastClickButton + 1, charX + 1, charY + 1 );
                    m_lastClickButton = -1;
                }

                int wheelChange = Mouse.getEventDWheel();
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

                handleMouseMove( computer, charX, charY );
            }
        }
        else // The mouse has moved out of the terminal, send a -1, -1 mouse_move event
        {
            handleMouseMove( computer, -1, -1 );
        }
    }

    private void handleMouseMove( IComputer computer, int charX, int charY )
    {
        // Avoid sending mouse movement events if the cursor is under the same character.
        // Note that clients can also use the mouseMoveThrottle config options to disable
        // sending their mouse movements to a server entirely.
        if( ComputerCraft.mouseMoveThrottle >= 0 && (m_lastMouseX != charX || m_lastMouseY != charY) )
        {
            computer.mouseMove( charX, charY );
            m_lastMouseX = charX;
            m_lastMouseY = charY;
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
                FixedWidthFontRenderer.drawTerminal(
                    startX + topMargin, startY + bottomMargin,
                    terminal, !computer.isColour(), topMargin, bottomMargin, leftMargin, rightMargin
                );
            }
            else
            {
                FixedWidthFontRenderer.drawEmptyTerminal( startX, startY, getWidth(), getHeight() );
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
