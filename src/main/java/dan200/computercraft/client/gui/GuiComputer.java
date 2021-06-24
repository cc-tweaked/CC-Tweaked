/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;

public final class GuiComputer<T extends ContainerComputerBase> extends ComputerScreenBase<T>
{
    private final int termWidth;
    private final int termHeight;

    private GuiComputer(
        T container, PlayerInventory player, ITextComponent title, int termWidth, int termHeight
    )
    {
        super( container, player, title, BORDER );
        this.termWidth = termWidth;
        this.termHeight = termHeight;

        imageWidth = WidgetTerminal.getWidth( termWidth ) + BORDER * 2 + ComputerSidebar.WIDTH;
        imageHeight = WidgetTerminal.getHeight( termHeight ) + BORDER * 2;
    }

    public static GuiComputer<ContainerComputer> create( ContainerComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight
        );
    }

    public static GuiComputer<ContainerPocketComputer> createPocket( ContainerPocketComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight
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
    protected WidgetTerminal createTerminal()
    {
        return new WidgetTerminal( computer,
            leftPos + ComputerSidebar.WIDTH + BORDER, topPos + BORDER, termWidth, termHeight
        );
    }

    @Override
    public void renderBg( @Nonnull MatrixStack stack, float partialTicks, int mouseX, int mouseY )
    {
        // Draw a border around the terminal
        RenderSystem.color4f( 1, 1, 1, 1 );
        minecraft.getTextureManager().bind( ComputerBorderRenderer.getTexture( family ) );
        ComputerBorderRenderer.render( terminal.x, terminal.y, getBlitOffset(), terminal.getWidth(), terminal.getHeight() );
        ComputerSidebar.renderBackground( stack, leftPos, topPos + sidebarYOffset );
    }
}
