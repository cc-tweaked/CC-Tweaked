/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiTurtle extends GuiContainer
{
    private static final ResourceLocation background = new ResourceLocation( "computercraft", "textures/gui/turtle.png" );
    private static final ResourceLocation backgroundAdvanced = new ResourceLocation( "computercraft", "textures/gui/turtle_advanced.png" );

    protected World m_world;
    protected ContainerTurtle m_container;

    protected final ComputerFamily m_family;
    protected final ITurtleAccess m_turtle;
    protected final ClientComputer m_computer;
    protected WidgetTerminal m_terminalGui;

    public GuiTurtle( World world, InventoryPlayer inventoryplayer, TileTurtle turtle )
    {
        this( world, turtle, new ContainerTurtle( inventoryplayer, turtle.getAccess() ) );
    }

    protected GuiTurtle( World world, TileTurtle turtle, ContainerTurtle container )
    {
        super( container );

        m_world = world;
        m_container = container;
        m_family = turtle.getFamily();
        m_turtle = turtle.getAccess();
        m_computer = turtle.getClientComputer();

        xSize = 254;
        ySize = 217;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        Keyboard.enableRepeatEvents( true );
        m_terminalGui = new WidgetTerminal(
            (width - xSize) / 2 + 8,
            (height - ySize) / 2 + 8,
            ComputerCraft.terminalWidth_turtle,
            ComputerCraft.terminalHeight_turtle,
            () -> m_computer,
            2, 2, 2, 2
        );
        m_terminalGui.setAllowFocusLoss( false );
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents( false );
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        m_terminalGui.update();
    }

    @Override
    protected void keyTyped( char c, int k ) throws IOException
    {
        if( k == 1 )
        {
            super.keyTyped( c, k );
        }
        else
        {
            if( m_terminalGui.onKeyTyped( c, k ) ) keyHandled = true;
        }
    }

    @Override
    protected void mouseClicked( int x, int y, int button ) throws IOException
    {
        super.mouseClicked( x, y, button );
        m_terminalGui.mouseClicked( x, y, button );
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int x = Mouse.getEventX() * this.width / mc.displayWidth;
        int y = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
        m_terminalGui.handleMouseInput( x, y );
    }

    @Override
    public void handleKeyboardInput() throws IOException
    {
        super.handleKeyboardInput();
        if( m_terminalGui.onKeyboardInput() ) keyHandled = true;
    }

    protected void drawSelectionSlot( boolean advanced )
    {
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        // Draw selection slot
        int slot = m_container.getSelectedSlot();
        if( slot >= 0 )
        {
            GlStateManager.color( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = (slot % 4);
            int slotY = (slot / 4);
            this.mc.getTextureManager().bindTexture( advanced ? backgroundAdvanced : background );
            drawTexturedModalRect( x + m_container.m_turtleInvStartX - 2 + slotX * 18, y + m_container.m_playerInvStartY - 2 + slotY * 18, 0, 217, 24, 24 );
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer( float f, int mouseX, int mouseY )
    {
        // Draw term
        boolean advanced = (m_family == ComputerFamily.Advanced);
        m_terminalGui.draw( Minecraft.getMinecraft(), 0, 0, mouseX, mouseY );

        // Draw border/inventory
        GlStateManager.color( 1.0F, 1.0F, 1.0F, 1.0F );
        this.mc.getTextureManager().bindTexture( advanced ? backgroundAdvanced : background );
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect( x, y, 0, 0, xSize, ySize );

        drawSelectionSlot( advanced );
    }

    @Override
    public void drawScreen( int mouseX, int mouseY, float partialTicks )
    {
        drawDefaultBackground();
        super.drawScreen( mouseX, mouseY, partialTicks );
        renderHoveredToolTip( mouseX, mouseY );
    }
}
