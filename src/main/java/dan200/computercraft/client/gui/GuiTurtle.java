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
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.turtle.inventory.ContainerTurtle.*;

public class GuiTurtle extends ComputerScreenBase<ContainerTurtle>
{
    private static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_normal.png" );
    private static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/turtle_advanced.png" );

    private static final int TEX_WIDTH = 254;
    private static final int TEX_HEIGHT = 217;

    private final ComputerFamily family;

    public GuiTurtle( ContainerTurtle container, PlayerInventory player, ITextComponent title )
    {
        super( container, player, title, BORDER );
        family = container.getFamily();

        imageWidth = TEX_WIDTH + ComputerSidebar.WIDTH;
        imageHeight = TEX_HEIGHT;
    }

    @Override
    protected WidgetTerminal createTerminal()
    {
        return new WidgetTerminal(
            computer, leftPos + BORDER + ComputerSidebar.WIDTH, topPos + BORDER,
            ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight
        );
    }

    @Override
    protected void renderBg( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        boolean advanced = family == ComputerFamily.ADVANCED;
        minecraft.getTextureManager().bind( advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL );
        blit( transform, leftPos + ComputerSidebar.WIDTH, topPos, 0, 0, TEX_WIDTH, TEX_HEIGHT );

        int slot = getMenu().getSelectedSlot();
        if( slot >= 0 )
        {
            RenderSystem.color4f( 1.0F, 1.0F, 1.0F, 1.0F );
            int slotX = slot % 4;
            int slotY = slot / 4;
            blit( transform,
                leftPos + TURTLE_START_X - 2 + slotX * 18, topPos + PLAYER_START_Y - 2 + slotY * 18,
                0, 217, 24, 24
            );
        }

        minecraft.getTextureManager().bind( advanced ? ComputerBorderRenderer.BACKGROUND_ADVANCED : ComputerBorderRenderer.BACKGROUND_NORMAL );
        ComputerSidebar.renderBackground( transform, leftPos, topPos + sidebarYOffset );
    }
}
