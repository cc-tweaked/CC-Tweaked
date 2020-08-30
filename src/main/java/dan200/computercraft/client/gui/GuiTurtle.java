/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.gui.widgets.WidgetWrapper;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

public class GuiTurtle extends HandledScreen<ContainerTurtle>
{
    private static final Identifier BACKGROUND_NORMAL = new Identifier( "computercraft", "textures/gui/turtle_normal.png" );
    private static final Identifier BACKGROUND_ADVANCED = new Identifier( "computercraft", "textures/gui/turtle_advanced.png" );

    private ContainerTurtle m_container;

    private final ComputerFamily m_family;
    private final ClientComputer m_computer;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiTurtle( ContainerTurtle container, PlayerInventory player, Text title )
    {
        super( container, player, title );

        m_container = container;
        m_family = container.getFamily();
        m_computer = (ClientComputer) container.getComputer();

        backgroundWidth = 254;
        backgroundHeight = 217;
    }

    @Override
    protected void init()
    {
        super.init();
        client.keyboard.setRepeatEvents( true );

        int termPxWidth = ComputerCraft.turtleTermWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.turtleTermHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        terminal = new WidgetTerminal(
            client, () -> m_computer,
            ComputerCraft.turtleTermWidth,
            ComputerCraft.turtleTermHeight,
            2, 2, 2, 2
        );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 8 + x, 2 + 8 + y, termPxWidth, termPxHeight );

        children.add( terminalWrapper );
        setFocused( terminalWrapper );
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
    protected void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        // Draw term
        Identifier texture = m_family == ComputerFamily.ADVANCED ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL;
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw border/inventory
        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        client.getTextureManager().bindTexture( texture );
        drawTexture( transform, x, y, 0, 0, backgroundWidth, backgroundHeight );

        // Draw selection slot
        int slot = m_container.getSelectedSlot();
        if( slot >= 0 )
        {
            int slotX = slot % 4;
            int slotY = slot / 4;
            drawTexture( transform,
                x + ContainerTurtle.TURTLE_START_X - 2 + slotX * 18,
                y + ContainerTurtle.PLAYER_START_Y - 2 + slotY * 18,
                0, 217, 24, 24
            );
        }
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( stack, mouseX, mouseY );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }
}
