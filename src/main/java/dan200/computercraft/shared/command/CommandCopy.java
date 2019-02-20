/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class CommandCopy
{
    public static void register( CommandDispatcher<CommandSource> registry )
    {
        registry.register( literal( "computercraft" )
            .then( literal( "copy" ) )
            .then( argument( "message", StringArgumentType.greedyString() ) )
            .executes( context -> {
                Minecraft.getInstance().keyboardListener.setClipboardString( context.getArgument( "message", String.class ) );
                return 1;
            } )
        );
    }

    private CommandCopy()
    {
    }

    public static ITextComponent createCopyText( String text )
    {
        TextComponentString name = new TextComponentString( text );
        name.getStyle()
            .setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/computercraft copy " + text ) )
            .setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation( "gui.computercraft.tooltip.copy" ) ) );
        return name;
    }
}
