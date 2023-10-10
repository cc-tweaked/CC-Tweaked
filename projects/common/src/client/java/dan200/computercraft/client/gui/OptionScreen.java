// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * A screen which displays a series of buttons (such as a yes/no prompt).
 * <p>
 * When closed, it returns to the previous screen.
 */
public final class OptionScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("computercraft", "textures/gui/blank_screen.png");

    public static final int BUTTON_WIDTH = 100;
    public static final int BUTTON_HEIGHT = 20;

    private static final int PADDING = 16;
    private static final int FONT_HEIGHT = 9;

    private int x;
    private int y;
    private int innerWidth;
    private int innerHeight;

    private @Nullable MultiLineLabel messageRenderer;
    private final Component message;
    private final List<AbstractWidget> buttons;
    private final Runnable exit;

    private final Screen originalScreen;

    private OptionScreen(Component title, Component message, List<AbstractWidget> buttons, Runnable exit, Screen originalScreen) {
        super(title);
        this.message = message;
        this.buttons = buttons;
        this.exit = exit;
        this.originalScreen = originalScreen;
    }

    public static void show(Minecraft minecraft, Component title, Component message, List<AbstractWidget> buttons, Runnable exit) {
        minecraft.setScreen(new OptionScreen(title, message, buttons, exit, unwrap(minecraft.screen)));
    }

    public static Screen unwrap(Screen screen) {
        return screen instanceof OptionScreen option ? option.getOriginalScreen() : screen;
    }

    @Override
    public void init() {
        super.init();

        var buttonWidth = BUTTON_WIDTH * buttons.size() + PADDING * (buttons.size() - 1);
        var innerWidth = this.innerWidth = Math.max(256, buttonWidth + PADDING * 2);

        messageRenderer = MultiLineLabel.create(font, message, innerWidth - PADDING * 2);

        var textHeight = messageRenderer.getLineCount() * FONT_HEIGHT + PADDING * 2;
        innerHeight = textHeight + (buttons.isEmpty() ? 0 : buttons.get(0).getHeight()) + PADDING;

        x = (width - innerWidth) / 2;
        y = (height - innerHeight) / 2;

        var x = (width - buttonWidth) / 2;
        for (var button : buttons) {
            button.setPosition(x, y + textHeight);
            addRenderableWidget(button);

            x += BUTTON_WIDTH + PADDING;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        // Render the actual texture.
        graphics.blit(BACKGROUND, x, y, 0, 0, innerWidth, PADDING);
        graphics.blit(BACKGROUND,
            x, y + PADDING, 0, PADDING, innerWidth, innerHeight - PADDING * 2,
            innerWidth, PADDING
        );
        graphics.blit(BACKGROUND, x, y + innerHeight - PADDING, 0, 256 - PADDING, innerWidth, PADDING);

        assertNonNull(messageRenderer).renderLeftAlignedNoShadow(graphics, x + PADDING, y + PADDING, FONT_HEIGHT, 0x404040);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        exit.run();
    }

    public static AbstractWidget newButton(Component component, Button.OnPress clicked) {
        return Button.builder(component, clicked).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    public Screen getOriginalScreen() {
        return originalScreen;
    }
}
