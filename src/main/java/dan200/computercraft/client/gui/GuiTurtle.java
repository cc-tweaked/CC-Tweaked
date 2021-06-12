/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;

public class GuiTurtle extends GuiComputer<ContainerTurtle>
{
    private static final Identifier BACKGROUND_NORMAL = new Identifier( "computercraft", "textures/gui/turtle_normal.png" );
    private static final Identifier BACKGROUND_ADVANCED = new Identifier( "computercraft", "textures/gui/turtle_advanced.png" );
    private final ContainerTurtle container;

    public GuiTurtle( ContainerTurtle container, PlayerInventory player, Text title )
    {
        super( container, player, title, ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight );

        this.container = container;
    }

    @Override
    protected void init()
    {
        this.initTerminal( 8, 0, 80 );
    }

    @Override
    public void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
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
}
