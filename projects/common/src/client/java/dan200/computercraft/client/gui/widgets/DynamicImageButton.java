// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Version of {@link net.minecraft.client.gui.components.ImageButton} which allows changing some properties
 * dynamically.
 */
public class DynamicImageButton extends Button {
    private final ResourceLocation texture;
    private final IntSupplier xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;
    private final Supplier<HintedMessage> message;

    public DynamicImageButton(
        int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        OnPress onPress, HintedMessage message
    ) {
        this(
            x, y, width, height, () -> xTexStart, yTexStart, yDiffTex,
            texture, textureWidth, textureHeight,
            onPress, () -> message
        );
    }

    public DynamicImageButton(
        int x, int y, int width, int height, IntSupplier xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        OnPress onPress, Supplier<HintedMessage> message
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.texture = texture;
        this.message = message;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        var yTex = yTexStart;
        if (isHoveredOrFocused()) yTex += yDiffTex;

        graphics.blit(texture, getX(), getY(), xTexStart.getAsInt(), yTex, width, height, textureWidth, textureHeight);
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
