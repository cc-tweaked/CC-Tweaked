package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

/**
 * Represents the way a command was invoked, including the command sender, the current server and
 * the "path" to this command.
 */
public final class CommandContext
{
    private final MinecraftServer server;
    private final ICommandSender sender;
    private final List<ISubCommand> path;

    public CommandContext( MinecraftServer server, ICommandSender sender, ISubCommand initial )
    {
        this.server = server;
        this.sender = sender;
        this.path = Collections.singletonList( initial );
    }

    private CommandContext( MinecraftServer server, ICommandSender sender, List<ISubCommand> path )
    {
        this.server = server;
        this.sender = sender;
        this.path = path;
    }

    public CommandContext enter( ISubCommand child )
    {
        List<ISubCommand> newPath = Lists.newArrayListWithExpectedSize( path.size() + 1 );
        newPath.addAll( path );
        newPath.add( child );
        return new CommandContext( server, sender, newPath );
    }

    public CommandContext parent()
    {
        if( path.size() == 1 ) throw new IllegalStateException( "No parent command" );
        return new CommandContext( server, sender, path.subList( 0, path.size() - 1 ) );
    }

    public String getFullPath()
    {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for( ISubCommand command : path )
        {
            if( first )
            {
                first = false;
            }
            else
            {
                out.append( ' ' );
            }

            out.append( command.getName() );
        }

        return out.toString();
    }

    public String getFullUsage()
    {
        return "/" + getFullPath() + " " + path.get( path.size() - 1 ).getUsage( this );
    }

    public List<ISubCommand> getPath()
    {
        return Collections.unmodifiableList( path );
    }

    public String getRootCommand()
    {
        return path.get( 0 ).getName();
    }

    public MinecraftServer getServer()
    {
        return server;
    }

    public ICommandSender getSender()
    {
        return sender;
    }
}
