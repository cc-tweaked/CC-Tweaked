// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.command.text.TableFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A {@link TableFormatter} subclass which writes directly to {@linkplain ChatComponent the chat GUI}.
 * <p>
 * Each message written gets a special {@link GuiMessageTag}, so we can remove the previous table of the same
 * {@linkplain TableBuilder#getId() id}.
 */
public class ClientTableFormatter implements TableFormatter {
    public static final ClientTableFormatter INSTANCE = new ClientTableFormatter();

    private static Font renderer() {
        return Minecraft.getInstance().font;
    }

    @Override
    @Nullable
    public Component getPadding(Component component, int width) {
        var extraWidth = width - getWidth(component);
        if (extraWidth <= 0) return null;

        var renderer = renderer();

        float spaceWidth = renderer.width(" ");
        var spaces = Mth.floor(extraWidth / spaceWidth);
        var extra = extraWidth - (int) (spaces * spaceWidth);

        return ChatHelpers.coloured(StringUtils.repeat(' ', spaces) + StringUtils.repeat((char) 712, extra), ChatFormatting.GRAY);
    }

    @Override
    public int getColumnPadding() {
        return 3;
    }

    @Override
    public int getWidth(Component component) {
        return renderer().width(component);
    }

    @Override
    public void writeLine(String label, Component component) {
        var mc = Minecraft.getInstance();
        var chat = mc.gui.getChat();

        // TODO: Trim the text if it goes over the allowed length
        // int maxWidth = MathHelper.floor( chat.getChatWidth() / chat.getScale() );
        // List<ITextProperties> list = RenderComponentsUtil.wrapComponents( component, maxWidth, mc.fontRenderer );
        // if( !list.isEmpty() ) chat.printChatMessageWithOptionalDeletion( list.get( 0 ), id );
        chat.addMessage(component, null, createTag(label));
    }

    @Override
    public void display(TableBuilder table) {
        var chat = Minecraft.getInstance().gui.getChat();

        var tag = createTag(table.getId());
        if (chat.allMessages.removeIf(guiMessage -> guiMessage.tag() != null && Objects.equals(guiMessage.tag().logTag(), tag.logTag()))) {
            chat.refreshTrimmedMessage();
        }

        TableFormatter.super.display(table);
    }

    private static GuiMessageTag createTag(String id) {
        return new GuiMessageTag(0xa0a0a0, null, null, "ComputerCraft/" + id);
    }
}
