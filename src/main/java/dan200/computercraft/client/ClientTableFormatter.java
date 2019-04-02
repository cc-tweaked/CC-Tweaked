/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.command.text.TableFormatter;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

public class ClientTableFormatter implements TableFormatter
{
    public static final ClientTableFormatter INSTANCE = new ClientTableFormatter();

    private static Int2IntOpenHashMap lastHeights = new Int2IntOpenHashMap();

    private static FontRenderer renderer()
    {
        return Minecraft.getInstance().fontRenderer;
    }

    @Override
    @Nullable
    public ITextComponent getPadding( ITextComponent component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;

        FontRenderer renderer = renderer();

        float spaceWidth = renderer.getStringWidth( " " );
        int spaces = MathHelper.floor( extraWidth / spaceWidth );
        int extra = extraWidth - (int) (spaces * spaceWidth);

        return ChatHelpers.coloured( StringUtils.repeat( ' ', spaces ) + StringUtils.repeat( (char) 712, extra ), TextFormatting.GRAY );
    }

    @Override
    public int getColumnPadding()
    {
        return 3;
    }

    @Override
    public int getWidth( ITextComponent component )
    {
        return renderer().getStringWidth( component.getFormattedText() );
    }

    @Override
    public void writeLine( int id, ITextComponent component )
    {
        Minecraft mc = Minecraft.getInstance();
        GuiNewChat chat = mc.ingameGUI.getChatGUI();

        // Trim the text if it goes over the allowed length
        int maxWidth = MathHelper.floor( chat.getChatWidth() / chat.getScale() );
        List<ITextComponent> list = GuiUtilRenderComponents.splitText( component, maxWidth, mc.fontRenderer, false, false );
        if( !list.isEmpty() ) chat.printChatMessageWithOptionalDeletion( list.get( 0 ), id );
    }

    @Override
    public int display( TableBuilder table )
    {
        GuiNewChat chat = Minecraft.getInstance().ingameGUI.getChatGUI();

        int lastHeight = lastHeights.get( table.getId() );

        int height = TableFormatter.super.display( table );
        lastHeights.put( table.getId(), height );

        for( int i = height; i < lastHeight; i++ ) chat.deleteChatLine( i + table.getId() );
        return height;
    }
}
