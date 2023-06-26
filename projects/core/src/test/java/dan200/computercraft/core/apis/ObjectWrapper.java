// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.core.asm.LuaMethodSupplier;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;

import java.util.Map;

public class ObjectWrapper implements ILuaContext {
    private static final MethodSupplier<LuaMethod> LUA_METHODS = LuaMethodSupplier.create();

    private final Object object;
    private final Map<String, LuaMethod> methodMap;

    public ObjectWrapper(Object object) {
        this.object = object;
        methodMap = LUA_METHODS.getSelfMethods(object);
    }

    public Object[] call(String name, Object... args) throws LuaException {
        var method = methodMap.get(name);
        if (method == null) throw new IllegalStateException("No such method '" + name + "'");

        return method.apply(object, this, new ObjectArguments(args)).getResult();
    }

    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    public <T> T callOf(String name, Object... args) throws LuaException {
        return (T) call(name, args)[0];
    }

    public <T> T callOf(Class<T> klass, String name, Object... args) throws LuaException {
        return klass.cast(call(name, args)[0]);
    }

    @Override
    public long issueMainThreadTask(LuaTask task) {
        throw new IllegalStateException("Method should never queue events");
    }
}
