package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import static dan200.computercraft.core.apis.ArgumentHelper.optInt;
import static dan200.computercraft.core.apis.ArgumentHelper.optString;

public abstract class HandleGeneric implements ILuaObject
{
    private Closeable m_closable;
    private boolean m_open = true;

    protected HandleGeneric( @Nonnull Closeable closable )
    {
        this.m_closable = closable;
    }

    protected void checkOpen() throws LuaException
    {
        if( !m_open ) throw new LuaException( "attempt to use a closed file" );
    }

    protected final void close()
    {
        try
        {
            m_closable.close();
            m_open = false;
        }
        catch( IOException ignored )
        {
        }
        m_closable = null;
    }

    /**
     * Shared implementation for various file handle types
     *
     * @param channel The channel to seek in
     * @param args    The Lua arguments to process, like Lua's {@code file:seek}.
     * @return The new position of the file, or null if some error occured.
     * @throws LuaException If the arguments were invalid
     * @see <a href="https://www.lua.org/manual/5.1/manual.html#pdf-file:seek">{@code file:seek} in the Lua manual.</a>
     */
    protected static Object[] handleSeek( SeekableByteChannel channel, Object[] args ) throws LuaException
    {
        try
        {
            String whence = optString( args, 0, "cur" );
            long offset = optInt( args, 1, 0 );
            switch( whence )
            {
                case "set":
                    channel.position( offset );
                    break;
                case "cur":
                    channel.position( channel.position() + offset );
                    break;
                case "end":
                    channel.position( channel.size() + offset );
                    break;
                default:
                    throw new LuaException( "bad argument #1 to 'seek' (invalid option '" + whence + "'" );
            }

            return new Object[] { channel.position() };
        }
        catch( IllegalArgumentException e )
        {
            return new Object[] { false, "Position is negative" };
        }
        catch( IOException e )
        {
            return null;
        }
    }
}
