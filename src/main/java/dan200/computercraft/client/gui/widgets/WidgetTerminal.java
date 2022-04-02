/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.BitSet;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;

public class WidgetTerminal extends AbstractWidget
{
    private static final float TERMINATE_TIME = 0.5f;

    private final ClientComputer computer;

    // The positions of the actual terminal
    private final int innerX;
    private final int innerY;
    private final int innerWidth;
    private final int innerHeight;

    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;

    private int lastMouseButton = -1;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    private final BitSet keysDown = new BitSet( 256 );

    public WidgetTerminal( @Nonnull ClientComputer computer, int x, int y, int termWidth, int termHeight )
    {
        super( x, y, termWidth * FONT_WIDTH + MARGIN * 2, termHeight * FONT_HEIGHT + MARGIN * 2, TextComponent.EMPTY );

        this.computer = computer;

        innerX = x + MARGIN;
        innerY = y + MARGIN;
        innerWidth = termWidth * FONT_WIDTH;
        innerHeight = termHeight * FONT_HEIGHT;
    }

    @Override
    public boolean charTyped( char ch, int modifiers )
    {
        if( ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255 ) // printable chars in byte range
        {
            // Queue the "char" event
            computer.queueEvent( "char", new Object[] { Character.toString( ch ) } );
        }

        return true;
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        if( key == GLFW.GLFW_KEY_ESCAPE ) return false;
        if( (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 )
        {
            switch( key )
            {
                case GLFW.GLFW_KEY_T:
                    if( terminateTimer < 0 ) terminateTimer = 0;
                    return true;
                case GLFW.GLFW_KEY_S:
                    if( shutdownTimer < 0 ) shutdownTimer = 0;
                    return true;
                case GLFW.GLFW_KEY_R:
                    if( rebootTimer < 0 ) rebootTimer = 0;
                    return true;

                case GLFW.GLFW_KEY_V:
                    // Ctrl+V for paste
                    String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if( clipboard != null )
                    {
                        // Clip to the first occurrence of \r or \n
                        int newLineIndex1 = clipboard.indexOf( "\r" );
                        int newLineIndex2 = clipboard.indexOf( "\n" );
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
                        clipboard = SharedConstants.filterText( clipboard );
                        if( !clipboard.isEmpty() )
                        {
                            // Clip to 512 characters and queue the event
                            if( clipboard.length() > 512 ) clipboard = clipboard.substring( 0, 512 );
                            computer.queueEvent( "paste", new Object[] { clipboard } );
                        }

                        return true;
                    }
            }
        }

        if( key >= 0 && terminateTimer < 0 && rebootTimer < 0 && shutdownTimer < 0 )
        {
            // Queue the "key" event and add to the down set
            boolean repeat = keysDown.get( key );
            keysDown.set( key );
            computer.keyDown( key, repeat );
        }

        return true;
    }

    @Override
    public boolean keyReleased( int key, int scancode, int modifiers )
    {
        // Queue the "key_up" event and remove from the down set
        if( key >= 0 && keysDown.get( key ) )
        {
            keysDown.set( key, false );
            computer.keyUp( key );
        }

        switch( key )
        {
            case GLFW.GLFW_KEY_T:
                terminateTimer = -1;
                break;
            case GLFW.GLFW_KEY_R:
                rebootTimer = -1;
                break;
            case GLFW.GLFW_KEY_S:
                shutdownTimer = -1;
                break;
            case GLFW.GLFW_KEY_LEFT_CONTROL:
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                terminateTimer = rebootTimer = shutdownTimer = -1;
                break;
        }

        return true;
    }

    @Override
    public boolean mouseClicked( double mouseX, double mouseY, int button )
    {
        if( !inTermRegion( mouseX, mouseY ) ) return false;
        if( !computer.isColour() || button < 0 || button > 2 ) return false;

        Terminal term = computer.getTerminal();
        if( term != null )
        {
            int charX = (int) ((mouseX - innerX) / FONT_WIDTH);
            int charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
            charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
            charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

            computer.mouseClick( button + 1, charX + 1, charY + 1 );

            lastMouseButton = button;
            lastMouseX = charX;
            lastMouseY = charY;
        }

        return true;
    }

    @Override
    public boolean mouseReleased( double mouseX, double mouseY, int button )
    {
        if( !inTermRegion( mouseX, mouseY ) ) return false;
        if( !computer.isColour() || button < 0 || button > 2 ) return false;

        Terminal term = computer.getTerminal();
        if( term != null )
        {
            int charX = (int) ((mouseX - innerX) / FONT_WIDTH);
            int charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
            charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
            charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

            if( lastMouseButton == button )
            {
                computer.mouseUp( lastMouseButton + 1, charX + 1, charY + 1 );
                lastMouseButton = -1;
            }

            lastMouseX = charX;
            lastMouseY = charY;
        }

        return false;
    }

    @Override
    public boolean mouseDragged( double mouseX, double mouseY, int button, double v2, double v3 )
    {
        if( !inTermRegion( mouseX, mouseY ) ) return false;
        if( !computer.isColour() || button < 0 || button > 2 ) return false;

        Terminal term = computer.getTerminal();
        if( term != null )
        {
            int charX = (int) ((mouseX - innerX) / FONT_WIDTH);
            int charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
            charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
            charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

            if( button == lastMouseButton && (charX != lastMouseX || charY != lastMouseY) )
            {
                computer.mouseDrag( button + 1, charX + 1, charY + 1 );
                lastMouseX = charX;
                lastMouseY = charY;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled( double mouseX, double mouseY, double delta )
    {
        if( !inTermRegion( mouseX, mouseY ) ) return false;
        if( !computer.isColour() || delta == 0 ) return false;

        Terminal term = computer.getTerminal();
        if( term != null )
        {
            int charX = (int) ((mouseX - innerX) / FONT_WIDTH);
            int charY = (int) ((mouseY - innerY) / FONT_HEIGHT);
            charX = Math.min( Math.max( charX, 0 ), term.getWidth() - 1 );
            charY = Math.min( Math.max( charY, 0 ), term.getHeight() - 1 );

            computer.mouseScroll( delta < 0 ? 1 : -1, charX + 1, charY + 1 );

            lastMouseX = charX;
            lastMouseY = charY;
        }

        return true;
    }

    private boolean inTermRegion( double mouseX, double mouseY )
    {
        return active && visible && mouseX >= innerX && mouseY >= innerY && mouseX < innerX + innerWidth && mouseY < innerY + innerHeight;
    }

    public void update()
    {
        if( terminateTimer >= 0 && terminateTimer < TERMINATE_TIME && (terminateTimer += 0.05f) > TERMINATE_TIME )
        {
            computer.queueEvent( "terminate" );
        }

        if( shutdownTimer >= 0 && shutdownTimer < TERMINATE_TIME && (shutdownTimer += 0.05f) > TERMINATE_TIME )
        {
            computer.shutdown();
        }

        if( rebootTimer >= 0 && rebootTimer < TERMINATE_TIME && (rebootTimer += 0.05f) > TERMINATE_TIME )
        {
            computer.reboot();
        }
    }

    @Override
    public void onFocusedChanged( boolean focused )
    {
        if( !focused )
        {
            // When blurring, we should make all keys go up
            for( int key = 0; key < keysDown.size(); key++ )
            {
                if( keysDown.get( key ) ) computer.keyUp( key );
            }
            keysDown.clear();

            // When blurring, we should make the last mouse button go up
            if( lastMouseButton > 0 )
            {
                computer.mouseUp( lastMouseButton + 1, lastMouseX + 1, lastMouseY + 1 );
                lastMouseButton = -1;
            }

            shutdownTimer = terminateTimer = rebootTimer = -1;
        }
    }

    @Override
    public void render( @Nonnull PoseStack transform, int mouseX, int mouseY, float partialTicks )
    {
        if( !visible ) return;
        Matrix4f matrix = transform.last().pose();
        Terminal terminal = computer.getTerminal();

        var bufferSource = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        var emitter = FixedWidthFontRenderer.toVertexConsumer( matrix, bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH ) );

        if( terminal != null )
        {
            boolean greyscale = !computer.isColour();
            FixedWidthFontRenderer.drawTerminal(
                emitter,
                (float) innerX, (float) innerY, terminal, greyscale, (float) MARGIN, (float) MARGIN, (float) MARGIN, (float) MARGIN
            );
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal( emitter, (float) x, (float) y, (float) width, (float) height );
        }

        bufferSource.endBatch();
    }

    @Override
    public void updateNarration( @Nonnull NarrationElementOutput output )
    {
        // I'm not sure what the right option is here.
    }

    public static int getWidth( int termWidth )
    {
        return termWidth * FONT_WIDTH + MARGIN * 2;
    }

    public static int getHeight( int termHeight )
    {
        return termHeight * FONT_HEIGHT + MARGIN * 2;
    }
}
