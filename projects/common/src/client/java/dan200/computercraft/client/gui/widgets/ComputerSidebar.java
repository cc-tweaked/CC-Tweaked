// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui.widgets;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.gui.widgets.DynamicImageButton.HintedMessage;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Registers buttons to interact with a computer.
 */
public final class ComputerSidebar {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "textures/gui/buttons.png");

    private static final int TEX_SIZE = 64;

    private static final int ICON_WIDTH = 12;
    private static final int ICON_HEIGHT = 12;
    private static final int ICON_MARGIN = 2;

    private static final int ICON_TEX_Y_DIFF = 14;

    private static final int CORNERS_BORDER = 3;
    private static final int FULL_BORDER = CORNERS_BORDER + ICON_MARGIN;

    private static final int BUTTONS = 2;
    private static final int HEIGHT = (ICON_HEIGHT + ICON_MARGIN * 2) * BUTTONS + CORNERS_BORDER * 2;

    private ComputerSidebar() {
    }

    public static void addButtons(BooleanSupplier isOn, InputHandler input, Consumer<AbstractWidget> add, int x, int y) {
        x += CORNERS_BORDER + 1;
        y += CORNERS_BORDER + ICON_MARGIN;

        var turnOn = new HintedMessage(Component.translatable("gui.computercraft.tooltip.turn_on"), (Component) null);
        var turnOff = new HintedMessage(
            Component.translatable("gui.computercraft.tooltip.turn_off"),
            Component.translatable("gui.computercraft.tooltip.turn_off.key")
        );
        add.accept(new DynamicImageButton(
            x, y, ICON_WIDTH, ICON_HEIGHT, () -> isOn.getAsBoolean() ? 15 : 1, 1, ICON_TEX_Y_DIFF,
            TEXTURE, TEX_SIZE, TEX_SIZE, b -> toggleComputer(isOn, input),
            () -> isOn.getAsBoolean() ? turnOff : turnOn
        ));

        y += ICON_HEIGHT + ICON_MARGIN * 2;

        add.accept(new DynamicImageButton(
            x, y, ICON_WIDTH, ICON_HEIGHT, 29, 1, ICON_TEX_Y_DIFF,
            TEXTURE, TEX_SIZE, TEX_SIZE, b -> input.queueEvent("terminate"),
            new HintedMessage(
                Component.translatable("gui.computercraft.tooltip.terminate"),
                Component.translatable("gui.computercraft.tooltip.terminate.key")
            )
        ));
    }

    public static void renderBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture,
            x, y, 0, 102, AbstractComputerMenu.SIDEBAR_WIDTH, FULL_BORDER,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );

        graphics.blit(texture,
            x, y + FULL_BORDER, AbstractComputerMenu.SIDEBAR_WIDTH, HEIGHT - FULL_BORDER * 2,
            0, 107, AbstractComputerMenu.SIDEBAR_WIDTH, 4,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );

        graphics.blit(texture,
            x, y + HEIGHT - FULL_BORDER, 0, 111, AbstractComputerMenu.SIDEBAR_WIDTH, FULL_BORDER,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );
    }

    private static void toggleComputer(BooleanSupplier isOn, InputHandler input) {
        if (isOn.getAsBoolean()) {
            input.shutdown();
        } else {
            input.turnOn();
        }
    }
}
