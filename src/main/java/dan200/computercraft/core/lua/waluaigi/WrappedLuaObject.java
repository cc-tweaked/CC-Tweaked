package dan200.computercraft.core.lua.waluaigi;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.NamedMethod;

import javax.annotation.Nonnull;
import java.util.List;

class WrappedLuaObject implements IDynamicLuaObject
{
    private final Object object;
    private final List<NamedMethod<LuaMethod>> methods;

    public WrappedLuaObject( Object object, List<NamedMethod<LuaMethod>> methods )
    {
        this.object = object;
        this.methods = methods;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        // TODO: Make this less allocation happy.
        String[] methods = new String[this.methods.size()];
        int i = 0;
        for( NamedMethod<LuaMethod> method : this.methods ) methods[i++] = method.getName();
        return methods;
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull ILuaContext context, int method, @Nonnull IArguments arguments ) throws LuaException
    {
        if( method < 0 || method >= methods.size() ) throw new LuaException( "Unknown method" );
        return methods.get( method ).getMethod().apply( object, context, arguments );
    }
}
