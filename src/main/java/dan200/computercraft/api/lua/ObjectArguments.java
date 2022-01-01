/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link IArguments} which wraps an array of {@link Object}.
 */
public final class ObjectArguments implements IArguments
{
    private static final IArguments EMPTY = new ObjectArguments();

    private boolean released = false;
    private final List<Object> args;

    @Deprecated
    @SuppressWarnings( "unused" )
    public ObjectArguments( IArguments arguments )
    {
        throw new IllegalStateException();
    }

    public ObjectArguments( Object... args )
    {
        this.args = Arrays.asList( args );
    }

    public ObjectArguments( List<Object> args )
    {
        this.args = Objects.requireNonNull( args );
    }

    @Override
    public int count()
    {
        return args.size();
    }

    @Override
    public IArguments drop( int count )
    {
        if( count < 0 ) throw new IllegalStateException( "count cannot be negative" );
        if( count == 0 ) return this;
        if( count >= args.size() ) return EMPTY;

        return new ObjectArguments( args.subList( count, args.size() ) );
    }

    @Nullable
    @Override
    public Object get( int index )
    {
        return index >= args.size() ? null : args.get( index );
    }

    @Override
    public Object[] getAll()
    {
        return args.toArray();
    }

    @Nonnull
    @Override
    public LuaTable<?, ?> getTableUnsafe( int index ) throws LuaException
    {
        if( released )
        {
            throw new IllegalStateException( "Cannot use getTableUnsafe after IArguments has been released" );
        }

        return IArguments.super.getTableUnsafe( index );
    }

    @Nonnull
    @Override
    public Optional<LuaTable<?, ?>> optTableUnsafe( int index ) throws LuaException
    {
        if( released )
        {
            throw new IllegalStateException( "Cannot use optTableUnsafe after IArguments has been released" );
        }

        return IArguments.super.optTableUnsafe( index );
    }

    @Override
    public void releaseImmediate()
    {
        released = true;
    }
}
