package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.ICallContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.apis.ArgumentHelper;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryOutputHandle extends HandleGeneric
{
    private final OutputStream m_writer;

    public BinaryOutputHandle( OutputStream writer )
    {
        super( writer );
        this.m_writer = writer;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "write",
            "flush",
            "close",
        };
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull ICallContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
                // write
                checkOpen();
                try
                {
                    if( args.length > 0 && args[ 0 ] instanceof Number )
                    {
                        int number = ((Number) args[ 0 ]).intValue();
                        m_writer.write( number );
                    }
                    else if( args.length > 0 && args[ 0 ] instanceof String )
                    {
                        String value = (String) args[ 0 ];
                        m_writer.write( StringUtil.encodeString( value ) );
                    }
                    else
                    {
                        throw ArgumentHelper.badArgument( 0, "string or number", args.length > 0 ? args[ 0 ] : null );
                    }
                    return MethodResult.empty();
                }
                catch( IOException e )
                {
                    throw new LuaException( e.getMessage() );
                }
            case 1:
                // flush
                checkOpen();
                try
                {
                    m_writer.flush();
                    return MethodResult.empty();
                }
                catch( IOException e )
                {
                    return MethodResult.empty();
                }
            case 2:
                //close
                close();
                return MethodResult.empty();
            default:
                return MethodResult.empty();
        }
    }
}
