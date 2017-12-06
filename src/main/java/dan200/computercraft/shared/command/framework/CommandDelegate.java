package dan200.computercraft.shared.command.framework;

import dan200.computercraft.ComputerCraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link net.minecraft.command.ICommand} which delegates to a {@link ISubCommand}.
 */
public class CommandDelegate implements ICommand
{
    private final ISubCommand command;

    public CommandDelegate( ISubCommand command )
    {
        this.command = command;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return command.getName();
    }

    @Nonnull
    @Override
    public String getUsage( @Nonnull ICommandSender sender )
    {
        return "/" + command.getName() + " " + command.getUsage( new CommandContext( sender.getServer(), sender, command ) );
    }

    @Nonnull
    @Override
    public List<String> getAliases()
    {
        return Collections.emptyList();
    }

    @Override
    public void execute( @Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args ) throws CommandException
    {
        try
        {
            command.execute( new CommandContext( server, sender, command ), Arrays.asList( args ) );
        }
        catch( CommandException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            ComputerCraft.log.error( "Unhandled exception in command", e );
            throw new CommandException( "Unhandled exception: " + e.toString() );
        }
    }

    @Nonnull
    @Override
    public List<String> getTabCompletions( @Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos pos )
    {
        return command.getCompletion( new CommandContext( server, sender, command ), Arrays.asList( args ) );
    }

    @Override
    public boolean checkPermission( @Nonnull MinecraftServer server, @Nonnull ICommandSender sender )
    {
        return command.checkPermission( new CommandContext( server, sender, command ) );
    }

    @Override
    public boolean isUsernameIndex( @Nonnull String[] args, int index )
    {
        return false;
    }

    @Override
    public int compareTo( @Nonnull ICommand o )
    {
        return getName().compareTo( o.getName() );
    }
}
