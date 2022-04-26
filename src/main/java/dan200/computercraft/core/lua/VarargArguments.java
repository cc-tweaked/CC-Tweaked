/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaValues;
import org.squiddev.cobalt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Optional;

class VarargArguments implements IArguments
{
    static final IArguments EMPTY = new VarargArguments( Constants.NONE );

    boolean released;
    private final Varargs varargs;
    private Object[] cache;

    VarargArguments( Varargs varargs )
    {
        this.varargs = varargs;
    }

    @Override
    public int count()
    {
        return varargs.count();
    }

    @Nullable
    @Override
    public Object get( int index )
    {
        if( index < 0 || index >= varargs.count() ) return null;

        Object[] cache = this.cache;
        if( cache == null )
        {
            cache = this.cache = new Object[varargs.count()];
        }
        else
        {
            Object existing = cache[index];
            if( existing != null ) return existing;
        }

        return cache[index] = CobaltLuaMachine.toObject( varargs.arg( index + 1 ), null );
    }

    @Override
    public IArguments drop( int count )
    {
        if( count < 0 ) throw new IllegalStateException( "count cannot be negative" );
        if( count == 0 ) return this;
        return new VarargArguments( varargs.subargs( count + 1 ) );
    }

    @Override
    public double getDouble( int index ) throws LuaException
    {
        LuaValue value = varargs.arg( index + 1 );
        if( !(value instanceof LuaNumber) ) throw LuaValues.badArgument( index, "number", value.typeName() );
        return value.toDouble();
    }

    @Override
    public long getLong( int index ) throws LuaException
    {
        LuaValue value = varargs.arg( index + 1 );
        if( !(value instanceof LuaNumber) ) throw LuaValues.badArgument( index, "number", value.typeName() );
        return value instanceof LuaInteger ? value.toInteger() : (long) LuaValues.checkFinite( index, value.toDouble() );
    }

    @Nonnull
    @Override
    public ByteBuffer getBytes( int index ) throws LuaException
    {
        LuaValue value = varargs.arg( index + 1 );
        if( !(value instanceof LuaBaseString) ) throw LuaValues.badArgument( index, "string", value.typeName() );

        LuaString str = ((LuaBaseString) value).strvalue();
        return ByteBuffer.wrap( str.bytes, str.offset, str.length ).asReadOnlyBuffer();
    }

    @Override
    public Optional<ByteBuffer> optBytes( int index ) throws LuaException
    {
        LuaValue value = varargs.arg( index + 1 );
        if( value.isNil() ) return Optional.empty();
        if( !(value instanceof LuaBaseString) ) throw LuaValues.badArgument( index, "string", value.typeName() );

        LuaString str = ((LuaBaseString) value).strvalue();
        return Optional.of( ByteBuffer.wrap( str.bytes, str.offset, str.length ).asReadOnlyBuffer() );
    }

    @Nonnull
    @Override
    public dan200.computercraft.api.lua.LuaTable<?, ?> getTableUnsafe( int index ) throws LuaException
    {
        if( released )
        {
            throw new IllegalStateException( "Cannot use getTableUnsafe after IArguments has been released" );
        }

        LuaValue value = varargs.arg( index + 1 );
        if( !(value instanceof LuaTable) ) throw LuaValues.badArgument( index, "table", value.typeName() );
        return new TableImpl( this, (LuaTable) value );
    }

    @Nonnull
    @Override
    public Optional<dan200.computercraft.api.lua.LuaTable<?, ?>> optTableUnsafe( int index ) throws LuaException
    {
        if( released )
        {
            throw new IllegalStateException( "Cannot use optTableUnsafe after IArguments has been released" );
        }

        LuaValue value = varargs.arg( index + 1 );
        if( value.isNil() ) return Optional.empty();
        if( !(value instanceof LuaTable) ) throw LuaValues.badArgument( index, "table", value.typeName() );
        return Optional.of( new TableImpl( this, (LuaTable) value ) );
    }

    @Override
    public void releaseImmediate()
    {
        released = true;
    }
}
