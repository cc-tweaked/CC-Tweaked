/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A {@link Toast} implementation which displays an arbitrary message along with an optional {@link ItemStack}.
 */
public class ItemToast implements Toast {
    public static final Object TRANSFER_NO_RESPONSE_TOKEN = new Object();

    private static final long DISPLAY_TIME = 7000L;
    private static final int MAX_LINE_SIZE = 200;

    private static final int IMAGE_SIZE = 16;
    private static final int LINE_SPACING = 10;
    private static final int MARGIN = 8;

    private final ItemStack stack;
    private final Component title;
    private final List<FormattedCharSequence> message;
    private final Object token;
    private final int width;

    private boolean isNew = true;
    private long firstDisplay;

    public ItemToast(Minecraft minecraft, ItemStack stack, Component title, Component message, Object token) {
        this.stack = stack;
        this.title = title;
        this.token = token;

        var font = minecraft.font;
        this.message = font.split(message, MAX_LINE_SIZE);
        width = Math.max(MAX_LINE_SIZE, this.message.stream().mapToInt(font::width).max().orElse(MAX_LINE_SIZE)) + MARGIN * 3 + IMAGE_SIZE;
    }

    public void showOrReplace(ToastComponent toasts) {
        var existing = toasts.getToast(ItemToast.class, getToken());
        if (existing != null) {
            existing.isNew = true;
        } else {
            toasts.addToast(this);
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return MARGIN * 2 + LINE_SPACING + message.size() * LINE_SPACING;
    }

    @Nonnull
    @Override
    public Object getToken() {
        return token;
    }

    @Nonnull
    @Override
    public Visibility render(@Nonnull PoseStack transform, @Nonnull ToastComponent component, long time) {
        if (isNew) {

            firstDisplay = time;
            isNew = false;
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (width == 160 && message.size() <= 1) {
            component.blit(transform, 0, 0, 0, 64, width, height());
        } else {

            var height = height();

            var bottom = Math.min(4, height - 28);
            renderBackgroundRow(transform, component, width, 0, 0, 28);

            for (var i = 28; i < height - bottom; i += 10) {
                renderBackgroundRow(transform, component, width, 16, i, Math.min(16, height - i - bottom));
            }

            renderBackgroundRow(transform, component, width, 32 - bottom, height - bottom, bottom);
        }

        var textX = MARGIN;
        if (!stack.isEmpty()) {
            textX += MARGIN + IMAGE_SIZE;
            component.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(stack, MARGIN, MARGIN + height() / 2 - IMAGE_SIZE);
        }

        component.getMinecraft().font.draw(transform, title, textX, MARGIN, 0xff500050);
        for (var i = 0; i < message.size(); ++i) {
            component.getMinecraft().font.draw(transform, message.get(i), textX, (float) (LINE_SPACING + (i + 1) * LINE_SPACING), 0xff000000);
        }

        return time - firstDisplay < DISPLAY_TIME ? Visibility.SHOW : Visibility.HIDE;
    }

    private static void renderBackgroundRow(PoseStack transform, ToastComponent component, int x, int u, int y, int height) {
        var leftOffset = 5;
        var rightOffset = Math.min(60, x - leftOffset);

        component.blit(transform, 0, y, 0, 32 + u, leftOffset, height);
        for (var k = leftOffset; k < x - rightOffset; k += 64) {
            component.blit(transform, k, y, 32, 32 + u, Math.min(64, x - k - rightOffset), height);
        }

        component.blit(transform, x - rightOffset, y, 160 - rightOffset, 32 + u, rightOffset, height);
    }
}
