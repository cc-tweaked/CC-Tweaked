// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.booleans.Boolean2ObjectFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Version of {@link net.minecraft.client.gui.components.ImageButton} which allows changing some properties
 * dynamically.
 */
public class DynamicImageButton extends Button {
    private final Boolean2ObjectFunction<TextureAtlasSprite> texture;
    private final Supplier<HintedMessage> message;

    public DynamicImageButton(
        int x, int y, int width, int height, Boolean2ObjectFunction<TextureAtlasSprite> texture, OnPress onPress,
        HintedMessage message
    ) {
        this(x, y, width, height, texture, onPress, () -> message);
    }

    public DynamicImageButton(
        int x, int y, int width, int height,
        Boolean2ObjectFunction<TextureAtlasSprite> texture,
        OnPress onPress, Supplier<HintedMessage> message
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.message = message;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var texture = this.texture.get(isHoveredOrFocused());

        RenderSystem.disableDepthTest();
        graphics.blit(getX(), getY(), 0, width, height, texture);
        RenderSystem.enableDepthTest();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var message = this.message.get();
        setMessage(message.message());
        setTooltip(message.tooltip());
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    public record HintedMessage(Component message, Tooltip tooltip) {
        public HintedMessage(Component message, @Nullable Component hint) {
            this(
                message,
                hint == null
                    ? Tooltip.create(message)
                    : Tooltip.create(Component.empty().append(message).append("\n").append(hint.copy().withStyle(ChatFormatting.GRAY)), hint)
            );
        }
    }
}
