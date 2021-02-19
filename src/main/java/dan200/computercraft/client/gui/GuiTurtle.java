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
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

public class GuiTurtle extends ContainerScreen<ContainerTurtle>
{
    private static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_normal.png" );
    private static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_advanced.png" );

    private final ContainerTurtle container;

    private final ComputerFamily family;
    private final ClientComputer computer;

    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiTurtle( ContainerTurtle container, PlayerInventory player, ITextComponent title )
    {
        super( container, player, title );

        this.container = container;
        family = container.getFamily();
        computer = (ClientComputer) container.getComputer();

        imageWidth = 254;
        imageHeight = 217;
    }

    @Override
    protected void init()
    {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        int termPxWidth = ComputerCraft.turtleTermWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.turtleTermHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        terminal = new WidgetTerminal(
            minecraft, () -> computer,
            ComputerCraft.turtleTermWidth,
            ComputerCraft.turtleTermHeight,
            2, 2, 2, 2
        );
        terminalWrapper = new WidgetWrapper( terminal, 2 + 8 + leftPos, 2 + 8 + topPos, termPxWidth, termPxHeight );

        children.add( terminalWrapper );
        setFocused( terminalWrapper );
    }

    @Override
    public void removed()
    {
        super.removed();
        children.remove( terminal );
        terminal = null;
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
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

    private void drawSelectionSlot( boolean advanced )
    {
        // Draw selection slot
        int slot = container.getSelectedSlot();
        if( slot >= 0 )
        {
            RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = slot % 4;
            int slotY = slot / 4;
            minecraft.getTextureManager().bind( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
            blit( leftPos + ContainerTurtle.TURTLE_START_X - 2 + slotX * 18, topPos + ContainerTurtle.PLAYER_START_Y - 2 + slotY * 18, 0, 217, 24, 24 );
        }
    }

    @Override
    protected void renderBg( float partialTicks, int mouseX, int mouseY )
    {
        // Draw term
        boolean advanced = family == ComputerFamily.ADVANCED;
        terminal.draw( terminalWrapper.getX(), terminalWrapper.getY() );

        // Draw border/inventory
        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bind( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
        blit( leftPos, topPos, 0, 0, imageWidth, imageHeight );

        drawSelectionSlot( advanced );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        renderTooltip( mouseX, mouseY );
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }
}
