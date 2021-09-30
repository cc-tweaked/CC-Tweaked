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
import dan200.computercraft.client.render.RenderTypes;
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

import java.util.List;

import static dan200.computercraft.client.render.ComputerBorderRenderer.*;

public final class GuiComputer<T extends ContainerComputerBase> extends ComputerScreenBase<T>
{
    private final int termWidth;
    private final int termHeight;

    private GuiComputer( T container, PlayerInventory player, Text title, int termWidth, int termHeight )
    {
        super( container, player, title, BORDER );
        this.termWidth = termWidth;
        this.termHeight = termHeight;

        backgroundWidth = WidgetTerminal.getWidth( termWidth ) + BORDER * 2;
        backgroundHeight = WidgetTerminal.getHeight( termHeight ) + BORDER * 2;
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

    @Override
    protected WidgetTerminal createTerminal()
    {
        return new WidgetTerminal( computer,
            x + BORDER, y + BORDER, termWidth, termHeight
        );
    }
    @Override
    public void drawBackground( @Nonnull MatrixStack stack, float partialTicks, int mouseX, int mouseY )
    {
        ComputerBorderRenderer.render(
            getTexture(family), terminal.x, terminal.y, getZOffset(),
            RenderTypes.FULL_BRIGHT_LIGHTMAP, terminal.getWidth(), terminal.getHeight() );
    }
}
