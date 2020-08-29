/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import java.util.List;

import javax.annotation.Nullable;

import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.command.text.TableFormatter;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class ClientTableFormatter implements TableFormatter {
    public static final ClientTableFormatter INSTANCE = new ClientTableFormatter();

    private static Int2IntOpenHashMap lastHeights = new Int2IntOpenHashMap();

    @Override
    @Nullable
    public Text getPadding(Text component, int width) {
        int extraWidth = width - this.getWidth(component);
        if (extraWidth <= 0) {
            return null;
        }

        TextRenderer renderer = renderer();

        float spaceWidth = renderer.getCharWidth(' ');
        int spaces = MathHelper.floor(extraWidth / spaceWidth);
        int extra = extraWidth - (int) (spaces * spaceWidth);

        return ChatHelpers.coloured(StringUtils.repeat(' ', spaces) + StringUtils.repeat((char) 712, extra), Formatting.GRAY);
    }

    private static TextRenderer renderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    @Override
    public int getColumnPadding() {
        return 3;
    }

    @Override
    public int getWidth(Text component) {
        return renderer().getWidth(component.asFormattedString());
    }

    @Override
    public void writeLine(int id, Text component) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ChatHud chat = mc.inGameHud.getChatHud();

        // Trim the text if it goes over the allowed length
        int maxWidth = MathHelper.floor(chat.getWidth() / chat.getChatScale());
        List<Text> list = ChatMessages.breakRenderedChatMessageLines(component, maxWidth, mc.textRenderer, false, false);
        if (!list.isEmpty()) {
            chat.addMessage(list.get(0), id);
        }
    }

    @Override
    public int display(TableBuilder table) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();

        int lastHeight = lastHeights.get(table.getId());

        int height = TableFormatter.super.display(table);
        lastHeights.put(table.getId(), height);

        for (int i = height; i < lastHeight; i++) {
            chat.removeMessage(i + table.getId());
        }
        return height;
    }
}
