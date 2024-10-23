// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A {@link Toast} implementation which displays an arbitrary message along with an optional {@link ItemStack}.
 */
public class ItemToast implements Toast {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("toast/recipe");
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

    private boolean changed = true;
    private long lastChanged;
    private Visibility visibility = Visibility.HIDE;

    public ItemToast(Minecraft minecraft, ItemStack stack, Component title, Component message, Object token) {
        this.stack = stack;
        this.title = title;
        this.token = token;

        var font = minecraft.font;
        this.message = font.split(message, MAX_LINE_SIZE);
        width = Math.max(MAX_LINE_SIZE, this.message.stream().mapToInt(font::width).max().orElse(MAX_LINE_SIZE)) + MARGIN * 3 + IMAGE_SIZE;
    }

    public void showOrReplace(ToastManager toasts) {
        var existing = toasts.getToast(ItemToast.class, getToken());
        if (existing != null) {
            existing.changed = true;
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

    @Override
    public Object getToken() {
        return token;
    }

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long time) {
        if (changed) {
            lastChanged = time;
            changed = false;
        }
        visibility = time - lastChanged < DISPLAY_TIME * toastManager.getNotificationDisplayTimeMultiplier() ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, long time) {
        graphics.blitSprite(RenderType::guiTextured, TEXTURE, 0, 0, width(), height());

        var textX = MARGIN;
        if (!stack.isEmpty()) {
            textX += MARGIN + IMAGE_SIZE;
            graphics.renderFakeItem(stack, MARGIN, MARGIN + height() / 2 - IMAGE_SIZE);
        }

        graphics.drawString(font, title, textX, MARGIN, 0xff500050, false);
        for (var i = 0; i < message.size(); ++i) {
            graphics.drawString(font, message.get(i), textX, LINE_SPACING + (i + 1) * LINE_SPACING, 0xff000000, false);
        }
    }
}
