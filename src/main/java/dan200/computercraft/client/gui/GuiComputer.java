/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.gui.widgets.WidgetWrapper;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;
import static dan200.computercraft.client.render.ComputerBorderRenderer.MARGIN;

public class GuiComputer<T extends ContainerComputerBase> extends HandledScreen<T>
{
    protected final ComputerFamily family;
    protected final ClientComputer computer;
    private final int termWidth;
    private final int termHeight;

    protected WidgetTerminal terminal;
    protected WidgetWrapper terminalWrapper;

    protected GuiComputer( T container, PlayerInventory player, Text title, int termWidth, int termHeight )
    {
        super( container, player, title );
        this.family = container.getFamily();
        this.computer = (ClientComputer) container.getComputer();
        this.termWidth = termWidth;
        this.termHeight = termHeight;
        this.terminal = null;
    }

    public static GuiComputer<ContainerComputer> create( ContainerComputer container, PlayerInventory inventory, Text component )
    {
        return new GuiComputer<>( container, inventory, component, ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight );
    }

    public static GuiComputer<ContainerPocketComputer> createPocket( ContainerPocketComputer container, PlayerInventory inventory, Text component )
    {
        return new GuiComputer<>( container, inventory, component, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight );
    }

    public static GuiComputer<ContainerViewComputer> createView( ContainerViewComputer container, PlayerInventory inventory, Text component )
    {
        return new GuiComputer<>( container, inventory, component, container.getWidth(), container.getHeight() );
    }

    protected void initTerminal( int border, int widthExtra, int heightExtra )
    {
        client.keyboard.setRepeatEvents( true );

        int termPxWidth = termWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = termHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        backgroundWidth = termPxWidth + MARGIN * 2 + border * 2 + widthExtra;
        backgroundHeight = termPxHeight + MARGIN * 2 + border * 2 + heightExtra;

        super.init();

        terminal = new WidgetTerminal( client, () -> computer, termWidth, termHeight, MARGIN, MARGIN, MARGIN, MARGIN );
        terminalWrapper = new WidgetWrapper( terminal, MARGIN + border + x, MARGIN + border + y, termPxWidth, termPxHeight );

        children.add( terminalWrapper );
        setFocused( terminalWrapper );
    }

    @Override
    protected void init()
    {
        initTerminal( BORDER, 0, 0 );
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        this.renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( stack, mouseX, mouseY );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }

    @Override
    public void drawBackground( @Nonnull MatrixStack stack, float partialTicks, int mouseX, int mouseY )
    {
        // Draw terminal
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw a border around the terminal
        RenderSystem.color4f( 1, 1, 1, 1 );
        client.getTextureManager()
            .bindTexture( ComputerBorderRenderer.getTexture( family ) );
        ComputerBorderRenderer.render( terminalWrapper.getX() - MARGIN, terminalWrapper.getY() - MARGIN,
            getZOffset(), terminalWrapper.getWidth() + MARGIN * 2, terminalWrapper.getHeight() + MARGIN * 2 );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY )) || super.mouseDragged( x, y, button, deltaX, deltaY );
    }

    @Override
    public boolean mouseReleased( double mouseX, double mouseY, int button )
    {
        return (getFocused() != null && getFocused().mouseReleased( mouseX, mouseY, button )) || super.mouseReleased( x, y, button );
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
    public void removed()
    {
        super.removed();
        children.remove( terminal );
        terminal = null;
        client.keyboard.setRepeatEvents( false );
    }

    @Override
    public void tick()
    {
        super.tick();
        terminal.update();
    }
}
