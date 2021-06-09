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
    private final ComputerFamily family;
    private final ClientComputer computer;
    private final ContainerTurtle container;
    private WidgetTerminal terminal;
    private WidgetWrapper terminalWrapper;

    public GuiTurtle( ContainerTurtle container, PlayerInventory player, Text title )
    {
        super( container, player, title );

        this.container = container;
        this.family = container.getFamily();
        this.computer = (ClientComputer) container.getComputer();

        this.backgroundWidth = 254;
        this.backgroundHeight = 217;
    }

    @Override
    protected void init()
    {
        super.init();
        this.client.keyboard.setRepeatEvents( true );

        int termPxWidth = ComputerCraft.turtleTermWidth * FixedWidthFontRenderer.FONT_WIDTH;
        int termPxHeight = ComputerCraft.turtleTermHeight * FixedWidthFontRenderer.FONT_HEIGHT;

        this.terminal = new WidgetTerminal( this.client, () -> this.computer, ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight, 2, 2, 2, 2 );
        this.terminalWrapper = new WidgetWrapper( this.terminal, 2 + 8 + this.x, 2 + 8 + this.y, termPxWidth, termPxHeight );

        this.children.add( this.terminalWrapper );
        this.setFocused( this.terminalWrapper );
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        this.renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        this.drawMouseoverTooltip( stack, mouseX, mouseY );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }

    @Override
    protected void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        // Draw term
        Identifier texture = this.family == ComputerFamily.ADVANCED ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL;
        this.terminal.draw( this.terminalWrapper.getX(), this.terminalWrapper.getY() );

        // Draw border/inventory
        RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
        this.client.getTextureManager()
            .bindTexture( texture );
        this.drawTexture( transform, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight );

        // Draw selection slot
        int slot = this.container.getSelectedSlot();
        if( slot >= 0 )
        {
            int slotX = slot % 4;
            int slotY = slot / 4;
            this.drawTexture( transform, this.x + ContainerTurtle.TURTLE_START_X - 2 + slotX * 18, this.y + ContainerTurtle.PLAYER_START_Y - 2 + slotY * 18,
                0,
                217,
                24,
                24 );
        }
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (this.getFocused() != null && this.getFocused().mouseDragged( x, y, button, deltaX, deltaY )) || super.mouseDragged( x, y, button, deltaX, deltaY );
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && this.getFocused() != null && this.getFocused() == this.terminalWrapper )
        {
            return this.getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    public void removed()
    {
        super.removed();
        this.children.remove( this.terminal );
        this.terminal = null;
        this.client.keyboard.setRepeatEvents( false );
    }

    @Override
    public void tick()
    {
        super.tick();
        this.terminal.update();
    }
}
