/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.LuaMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ObjectWrapper implements ILuaContext {
    private final Object object;
    private final Map<String, LuaMethod> methodMap;

    public ObjectWrapper(Object object) {
        this.object = object;
        var dynamicMethods = object instanceof IDynamicLuaObject dynamic
            ? Objects.requireNonNull(dynamic.getMethodNames(), "Methods cannot be null")
            : LuaMethod.EMPTY_METHODS;

        var methods = LuaMethod.GENERATOR.getMethods(object.getClass());

        var methodMap = this.methodMap = new HashMap<>(methods.size() + dynamicMethods.length);
        for (var i = 0; i < dynamicMethods.length; i++) {
            methodMap.put(dynamicMethods[i], LuaMethod.DYNAMIC.get(i));
        }
        for (var method : methods) {
            methodMap.put(method.getName(), method.getMethod());
        }
    }

    public Object[] call(String name, Object... args) throws LuaException {
        var method = methodMap.get(name);
        if (method == null) throw new IllegalStateException("No such method '" + name + "'");

        return method.apply(object, this, new ObjectArguments(args)).getResult();
    }

    @SuppressWarnings("unchecked")
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
