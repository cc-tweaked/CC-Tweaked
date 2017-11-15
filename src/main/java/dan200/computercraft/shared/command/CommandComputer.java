package dan200.computercraft.shared.command;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

public class CommandComputer extends CommandBase
{
    @Override
    @Nonnull
    public String getName()
    {
        return "computer";
    }

    @Override
    @Nonnull
    public String getUsage( @Nonnull ICommandSender sender )
    {
        return "computer <id> <value1> [value2]...";
    }

    @Override
    public void execute( @Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args ) throws CommandException
    {
        if( args.length < 2 )
        {
            throw new CommandException( "Usage: /computer <id> <value1> [value2]..." );
        }
        try
        {
            ServerComputer computer = ComputerCraft.serverComputerRegistry.lookup( Integer.valueOf( args[ 0 ] ) );
            if( computer != null && computer.getFamily() == ComputerFamily.Command )
            {
                computer.queueEvent( "computer_command", ArrayUtils.remove( args, 0 ) );
            }
            else
            {
                throw new CommandException( "Computer #" + args[ 0 ] + " is not a Command Computer" );
            }
        }
        catch( NumberFormatException e )
        {
            throw new CommandException( "Invalid ID" );
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
}
