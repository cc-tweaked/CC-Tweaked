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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.util.TextComponentUtil;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TextFormat;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

public class ClientTableFormatter implements TableFormatter
{
    public static final ClientTableFormatter INSTANCE = new ClientTableFormatter();

    private static Int2IntOpenHashMap lastHeights = new Int2IntOpenHashMap();

    private static TextRenderer renderer()
    {
        return MinecraftClient.getInstance().textRenderer;
    }

    @Override
    @Nullable
    public TextComponent getPadding( TextComponent component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;

        TextRenderer renderer = renderer();

        float spaceWidth = renderer.getCharWidth( ' ' );
        int spaces = MathHelper.floor( extraWidth / spaceWidth );
        int extra = extraWidth - (int) (spaces * spaceWidth);

        return ChatHelpers.coloured( StringUtils.repeat( ' ', spaces ) + StringUtils.repeat( (char) 712, extra ), TextFormat.GRAY );
    }

    @Override
    public int getColumnPadding()
    {
        return 3;
    }

    @Override
    public int getWidth( TextComponent component )
    {
        return renderer().getStringWidth( component.getFormattedText() );
    }

    @Override
    public void writeLine( int id, TextComponent component )
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ChatHud chat = mc.inGameHud.getChatHud();

        // Trim the text if it goes over the allowed length
        int maxWidth = MathHelper.floor( chat.getWidth() / chat.getScale() );
        List<TextComponent> list = TextComponentUtil.wrapLines( component, maxWidth, mc.textRenderer, false, false );
        if( !list.isEmpty() ) chat.addMessage( list.get( 0 ), id );
    }

    @Override
    public int display( TableBuilder table )
    {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();

        int lastHeight = lastHeights.get( table.getId() );

        int height = TableFormatter.super.display( table );
        lastHeights.put( table.getId(), height );

        for( int i = height; i < lastHeight; i++ ) chat.removeMessage( i + table.getId() );
        return height;
    }
}
