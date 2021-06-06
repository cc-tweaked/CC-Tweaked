/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

import static dan200.computercraft.shared.turtle.inventory.ContainerTurtle.*;

public class GuiTurtle extends ContainerScreen<ContainerTurtle>
{
    private static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_normal.png" );
    private static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_advanced.png" );

    private final ContainerTurtle container;

    private final ComputerFamily family;
    private final ClientComputer computer;

    private WidgetTerminal terminal;

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

        terminal = addButton( new WidgetTerminal(
            computer, leftPos + BORDER + ComputerSidebar.WIDTH, topPos + BORDER,
            ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight
        ) );
        ComputerSidebar.addButtons( this, computer, this::addButton, leftPos, topPos + BORDER );
        setFocused( terminal );
    }

    @Override
    public void removed()
    {
        super.removed();
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
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    protected void renderBg( float partialTicks, int mouseX, int mouseY )
    {
        boolean advanced = family == ComputerFamily.ADVANCED;

        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        minecraft.getTextureManager().bind( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
        blit( leftPos + ComputerSidebar.WIDTH, topPos, 0, 0, imageWidth, imageHeight );

        minecraft.getTextureManager().bind( advanced ? ComputerBorderRenderer.BACKGROUND_ADVANCED : ComputerBorderRenderer.BACKGROUND_NORMAL );
        ComputerSidebar.renderBackground( leftPos, topPos + BORDER );

        int slot = container.getSelectedSlot();
        if( slot >= 0 )
        {
            RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = slot % 4;
            int slotY = slot / 4;
            blit(
                leftPos + TURTLE_START_X - 2 + slotX * 18, topPos + PLAYER_START_Y - 2 + slotY * 18,
                0, 217, 24, 24
            );
        }
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        renderTooltip( mouseX, mouseY );

        for( Widget widget : buttons )
        {
            if( widget.isHovered() ) widget.renderToolTip( mouseX, mouseY );
        }
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }
}
