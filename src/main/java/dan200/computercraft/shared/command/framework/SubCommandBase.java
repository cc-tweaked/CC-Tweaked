/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.framework;

import dan200.computercraft.shared.command.UserLevel;

import javax.annotation.Nonnull;

public abstract class SubCommandBase implements ISubCommand
{
    private final String name;
    private final String id;
    private final UserLevel level;
    private ISubCommand parent;

    protected SubCommandBase( String name, UserLevel level )
    {
        this.name = name;
        this.id = name.replace( '-', '_' );
        this.level = level;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Nonnull
    @Override
    public String getFullName()
    {
        return parent == null ? id : parent.getFullName() + "." + id;
    }

    @Override
    public boolean checkPermission( @Nonnull CommandContext context )
    {
        return level.canExecute( context );
    }

    void setParent( ISubCommand parent )
    {
        if( this.parent != null ) throw new IllegalStateException( "Cannot have multiple parents" );
        this.parent = parent;
    }
}
