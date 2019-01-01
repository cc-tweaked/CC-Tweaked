/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.framework;

import dan200.computercraft.shared.command.UserLevel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public abstract class SubCommandBase implements ISubCommand
{
    private final String name;
    private final String usage;
    private final String synopsis;
    private final String description;
    private final UserLevel level;

    public SubCommandBase( String name, String usage, String synopsis, UserLevel level, String description )
    {
        this.name = name;
        this.usage = usage;
        this.synopsis = synopsis;
        this.description = description;
        this.level = level;
    }

    public SubCommandBase( String name, String synopsis, UserLevel level, String description )
    {
        this( name, "", synopsis, level, description );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Nonnull
    @Override
    public String getUsage( CommandContext context )
    {
        return usage;
    }

    @Nonnull
    @Override
    public String getSynopsis()
    {
        return synopsis;
    }

    @Nonnull
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean checkPermission( @Nonnull CommandContext context )
    {
        return level.canExecute( context );
    }

    @Nonnull
    @Override
    public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
    {
        return Collections.emptyList();
    }
}
