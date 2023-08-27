// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui.widgets;

import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.gui.widgets.DynamicImageButton.HintedMessage;
import dan200.computercraft.client.render.SpriteRenderer;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Registers buttons to interact with a computer.
 */
public final class ComputerSidebar {
    private static final int ICON_WIDTH = 12;
    private static final int ICON_HEIGHT = 12;
    private static final int ICON_MARGIN = 2;

    private static final int CORNERS_BORDER = 3;
    private static final int FULL_BORDER = CORNERS_BORDER + ICON_MARGIN;

    private static final int BUTTONS = 2;
    private static final int HEIGHT = (ICON_HEIGHT + ICON_MARGIN * 2) * BUTTONS + CORNERS_BORDER * 2;

    private static final int TEX_HEIGHT = 14;

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
            x, y, ICON_WIDTH, ICON_HEIGHT,
            h -> isOn.getAsBoolean() ? GuiSprites.TURNED_ON.get(h) : GuiSprites.TURNED_OFF.get(h),
            b -> toggleComputer(isOn, input),
            () -> isOn.getAsBoolean() ? turnOff : turnOn
        ));

        y += ICON_HEIGHT + ICON_MARGIN * 2;

        add.accept(new DynamicImageButton(
            x, y, ICON_WIDTH, ICON_HEIGHT,
            GuiSprites.TERMINATE::get,
            b -> input.queueEvent("terminate"),
            new HintedMessage(
                Component.translatable("gui.computercraft.tooltip.terminate"),
                Component.translatable("gui.computercraft.tooltip.terminate.key")
            )
        ));
    }

    public static void renderBackground(SpriteRenderer renderer, GuiSprites.ComputerTextures textures, int x, int y) {
        var texture = textures.sidebar();
        if (texture == null) throw new NullPointerException(textures + " has no sidebar texture");
        var sprite = GuiSprites.get(texture);

        renderer.blitVerticalSliced(sprite, x, y, AbstractComputerMenu.SIDEBAR_WIDTH, HEIGHT, FULL_BORDER, FULL_BORDER, TEX_HEIGHT);
    }

    private static void toggleComputer(BooleanSupplier isOn, InputHandler input) {
        if (isOn.getAsBoolean()) {
            input.shutdown();
        } else {
            input.turnOn();
        }
    }
}
