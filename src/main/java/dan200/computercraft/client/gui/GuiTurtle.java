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
import org.lwjgl.glfw.GLFW;

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

        containerWidth = 254;
        containerHeight = 217;
    }

    @Override
    protected void init()
    {
        super.init();
        minecraft.keyboard.enableRepeatEvents( true );

        int termPxWidth = ComputerCraft.terminalWidth_turtle * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.terminalHeight_turtle * FixedWidthFontRenderer.FONT_HEIGHT;

        terminal = new WidgetTerminal(
            minecraft, () -> m_computer,
            ComputerCraft.terminalWidth_turtle,
            ComputerCraft.terminalHeight_turtle,
            2, 2, 2, 2
        );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 8 + left, 2 + 8 + top, termPxWidth, termPxHeight );

        children.add( terminalWrapper );
        setFocused( terminalWrapper );
    }

    @Override
    public void removed()
    {
        super.removed();
        children.remove( terminal );
        terminal = null;
        minecraft.keyboard.enableRepeatEvents( false );
    }

    @Override
    public void tick()
    {
        super.tick();
        terminal.update();
    }

    private void drawSelectionSlot( boolean advanced )
    {
        // Draw selection slot
        int slot = m_container.getSelectedSlot();
        if( slot >= 0 )
        {
            GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = slot % 4;
            int slotY = slot / 4;
            minecraft.getTextureManager().bindTexture( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
            blit( left + m_container.m_turtleInvStartX - 2 + slotX * 18, top + m_container.m_playerInvStartY - 2 + slotY * 18, 0, 217, 24, 24 );
        }
    }

    @Override
    protected void drawBackground( float partialTicks, int mouseX, int mouseY )
    {
        // Draw term
        boolean advanced = m_family == ComputerFamily.Advanced;
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw border/inventory
        GlStateManager.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bindTexture( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
        blit( left, top, 0, 0, containerWidth, containerHeight );

        drawSelectionSlot( advanced );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( mouseX, mouseY );
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        return (key == GLFW.GLFW_KEY_TAB && getFocused() == terminalWrapper && terminalWrapper.keyPressed( key, scancode, modifiers ))
            || super.keyPressed( key, scancode, modifiers );
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
