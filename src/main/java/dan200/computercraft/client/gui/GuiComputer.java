/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;

public final class GuiComputer<T extends ContainerComputerBase> extends ComputerScreenBase<T>
{
    private final int termWidth;
    private final int termHeight;

    private GuiComputer(
        T container, Inventory player, Component title, int termWidth, int termHeight
    )
    {
        super( container, player, title, BORDER );
        this.termWidth = termWidth;
        this.termHeight = termHeight;

        imageWidth = WidgetTerminal.getWidth( termWidth ) + BORDER * 2 + ComputerSidebar.WIDTH;
        imageHeight = WidgetTerminal.getHeight( termHeight ) + BORDER * 2;
    }

    @Nonnull
    public static GuiComputer<ContainerComputerBase> create( ContainerComputerBase container, Inventory inventory, Component component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight
        );
    }

    @Nonnull
    public static GuiComputer<ContainerComputerBase> createPocket( ContainerComputerBase container, Inventory inventory, Component component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight
        );
    }

    @Nonnull
    public static GuiComputer<ContainerViewComputer> createView( ContainerViewComputer container, Inventory inventory, Component component )
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
    public void renderBg( @Nonnull PoseStack stack, float partialTicks, int mouseX, int mouseY )
    {
        // Draw a border around the terminal
        ComputerBorderRenderer.render(
            ComputerBorderRenderer.getTexture( family ), terminal.x, terminal.y, getBlitOffset(),
            RenderTypes.FULL_BRIGHT_LIGHTMAP, terminal.getWidth(), terminal.getHeight()
        );
        ComputerSidebar.renderBackground( stack, leftPos, topPos + sidebarYOffset );
    }
}
