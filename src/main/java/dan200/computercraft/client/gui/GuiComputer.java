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
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

public final class GuiComputer<T extends ContainerComputerBase> extends ContainerScreen<T>
{
    public static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_normal.png" );
    public static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_advanced.png" );
    public static final ResourceLocation BACKGROUND_COMMAND = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_command.png" );
    public static final ResourceLocation BACKGROUND_COLOUR = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_colour.png" );

    private final ComputerFamily m_family;
    private final ClientComputer m_computer;
    private final int m_termWidth;
    private final int m_termHeight;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    private GuiComputer(
        T container, PlayerInventory player, ITextComponent title, int termWidth, int termHeight
    )
    {
        super( container, player, title );
        m_family = container.getFamily();
        m_computer = (ClientComputer) container.getComputer();
        m_termWidth = termWidth;
        m_termHeight = termHeight;
        terminal = null;
    }

    public static GuiComputer<ContainerComputer> create( ContainerComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.terminalWidth_computer, ComputerCraft.terminalHeight_computer
        );
    }

    public static GuiComputer<ContainerPocketComputer> createPocket( ContainerPocketComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.terminalWidth_pocketComputer, ComputerCraft.terminalHeight_pocketComputer
        );
    }

    public static GuiComputer<ContainerViewComputer> createView( ContainerViewComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            container.getWidth(), container.getHeight()
        );
    }


    @Override
    protected void init()
    {
        minecraft.keyboardListener.enableRepeatEvents( true );

        int termPxWidth = m_termWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = m_termHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        xSize = termPxWidth + 4 + 24;
        ySize = termPxHeight + 4 + 24;

        super.init();

        terminal = new WidgetTerminal( minecraft, () -> m_computer, m_termWidth, m_termHeight, 2, 2, 2, 2 );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 12 + guiLeft, 2 + 12 + guiTop, termPxWidth, termPxHeight );

        children.add( terminalWrapper );
        setFocused( terminalWrapper );
    }

    @Override
    public void removed()
    {
        super.removed();
        children.remove( terminal );
        terminal = null;
        minecraft.keyboardListener.enableRepeatEvents( false );
    }

    @Override
    public void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminalWrapper )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    public void drawGuiContainerBackgroundLayer( float partialTicks, int mouseX, int mouseY )
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
                minecraft.getTextureManager().bindTexture( BACKGROUND_NORMAL );
                break;
            case Advanced:
                minecraft.getTextureManager().bindTexture( BACKGROUND_ADVANCED );
                break;
            case Command:
                minecraft.getTextureManager().bindTexture( BACKGROUND_COMMAND );
                break;
        }

        blit( startX - 12, startY - 12, 12, 28, 12, 12 );
        blit( startX - 12, endY, 12, 40, 12, 12 );
        blit( endX, startY - 12, 24, 28, 12, 12 );
        blit( endX, endY, 24, 40, 12, 12 );

        blit( startX, startY - 12, 0, 0, endX - startX, 12 );
        blit( startX, endY, 0, 12, endX - startX, 12 );

        blit( startX - 12, startY, 0, 28, 12, endY - startY );
        blit( endX, startY, 36, 28, 12, endY - startY );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        renderHoveredToolTip( mouseX, mouseY );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }
}
