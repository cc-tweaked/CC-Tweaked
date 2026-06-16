// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A screen which displays a series of buttons (such as a yes/no prompt).
 * <p>
 * When closed, it returns to the previous screen.
 */
public final class OptionScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("computercraft", "textures/gui/blank_screen.png");

    public static final int BUTTON_WIDTH = 100;
    public static final int BUTTON_HEIGHT = 20;

    private static final int PADDING = 16;
    private static final int FONT_HEIGHT = 9;

    private int x;
    private int y;
    private int innerWidth;
    private int innerHeight;

    private final Component message;
    private List<FormattedCharSequence> messageLines = List.of();
    private final List<AbstractWidget> buttons;

    private final Screen originalScreen;

    private OptionScreen(Component title, Component message, List<AbstractWidget> buttons, Screen originalScreen) {
        super(title);
        this.message = message;
        this.buttons = buttons;
        this.originalScreen = originalScreen;
    }

    public static void show(Minecraft minecraft, Screen originalScreen, Component title, Component message, List<AbstractWidget> buttons) {
        minecraft.gui.setScreen(new OptionScreen(title, message, buttons, unwrap(originalScreen)));
    }

    @Contract("!null -> !null")
    public static @Nullable Screen unwrap(@Nullable Screen screen) {
        return screen instanceof OptionScreen option ? option.getOriginalScreen() : screen;
    }

    @Override
    public void init() {
        super.init();

        var buttonWidth = BUTTON_WIDTH * buttons.size() + PADDING * (buttons.size() - 1);
        var innerWidth = this.innerWidth = Math.max(256, buttonWidth + PADDING * 2);

        messageLines = font.split(message, innerWidth - PADDING * 2);

        var textHeight = messageLines.size() * FONT_HEIGHT + PADDING * 2;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        // Render the actual texture.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, innerWidth, PADDING, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            x, y + PADDING, 0, PADDING, innerWidth, innerHeight - PADDING * 2,
            innerWidth, PADDING,
            256, 256
        );
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y + innerHeight - PADDING, 0, 256 - PADDING, innerWidth, PADDING, 256, 256);

        var textY = this.y + PADDING;
        for (var line : messageLines) {
            graphics.text(font, line, x, textY, ARGB.opaque(0x404040), false);
            textY += FONT_HEIGHT;
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(originalScreen);
    }

    public static AbstractWidget newButton(Component component, Button.OnPress clicked) {
        return Button.builder(component, clicked).bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    public Screen getOriginalScreen() {
        return originalScreen;
    }
}
