// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.SpriteRenderer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import dan200.computercraft.shared.turtle.inventory.TurtleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static dan200.computercraft.shared.turtle.inventory.TurtleMenu.*;

/**
 * The GUI for turtles.
 */
public class TurtleScreen extends AbstractComputerScreen<TurtleMenu> {
    private static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "textures/gui/turtle_normal.png");
    private static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation(ComputerCraftAPI.MOD_ID, "textures/gui/turtle_advanced.png");

    private static final int TEX_WIDTH = 278;
    private static final int TEX_HEIGHT = 217;

    private static final int FULL_TEX_SIZE = 512;

    public TurtleScreen(TurtleMenu container, Inventory player, Component title) {
        super(container, player, title, BORDER);

        imageWidth = TEX_WIDTH + AbstractComputerMenu.SIDEBAR_WIDTH;
        imageHeight = TEX_HEIGHT;
    }

    @Override
    protected TerminalWidget createTerminal() {
        return new TerminalWidget(terminalData, input, leftPos + BORDER + AbstractComputerMenu.SIDEBAR_WIDTH, topPos + BORDER);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        var advanced = family == ComputerFamily.ADVANCED;
        var texture = advanced ? BACKGROUND_ADVANCED : BACKGROUND_NORMAL;
        graphics.blit(texture, leftPos + AbstractComputerMenu.SIDEBAR_WIDTH, topPos, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, FULL_TEX_SIZE, FULL_TEX_SIZE);

        // Render selected slot
        var slot = getMenu().getSelectedSlot();
        if (slot >= 0) {
            var slotX = slot % 4;
            var slotY = slot / 4;
            graphics.blit(texture,
                leftPos + TURTLE_START_X - 2 + slotX * 18, topPos + PLAYER_START_Y - 2 + slotY * 18, 0,
                0, 217, 24, 24, FULL_TEX_SIZE, FULL_TEX_SIZE
            );
        }

        // Render sidebar
        var spriteRenderer = SpriteRenderer.createForGui(graphics, RenderTypes.GUI_SPRITES);
        ComputerSidebar.renderBackground(spriteRenderer, GuiSprites.getComputerTextures(family), leftPos, topPos + sidebarYOffset);
        graphics.flush(); // Flush to ensure background textures are drawn before foreground.
    }
}
