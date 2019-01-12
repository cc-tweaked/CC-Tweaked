/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.IClientCommand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class CommandCopy extends CommandBase implements IClientCommand
{
    public static final CommandCopy INSTANCE = new CommandCopy();

    /**
     * We start with a "~" so we're less likely to show up on completions.
     */
    private static final String NAME = "~computercraft_copy";

    private CommandCopy()
    {
    }

    @Override
    public boolean allowUsageWithoutPrefix( ICommandSender sender, String message )
    {
        return false;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return NAME;
    }

    @Nonnull
    @Override
    public String getUsage( @Nonnull ICommandSender sender )
    {
        return "/" + NAME + " <text>";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    @SideOnly( Side.CLIENT )
    public void execute( @Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args )
    {
        String message = String.join( " ", args );
        if( !message.isEmpty() ) GuiScreen.setClipboardString( message );
    }

    public static ITextComponent createCopyText( String text )
    {
        TextComponentString name = new TextComponentString( text );
        name.getStyle()
            .setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/" + NAME + " " + text ) )
            .setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation( "gui.computercraft:tooltip.copy" ) ) );
        return name;
    }
}
