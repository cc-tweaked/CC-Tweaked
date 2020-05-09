/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.lang.annotation.*;

/**
 * Used to mark a Java function which is callable from Lua.
 *
 * Methods annotated with {@link LuaFunction} must be public final instance methods. They can accept 0 or more
 * arguments, in any order, but must be of the form {@link ILuaContext} or {@code Object[]}. If this method is appears
 * on an instance of {@link IPeripheral}, {@link IComputerAccess} may also be used.
 *
 * The {@code Object[]} array specifies the arguments to this function. Each argument may be of the following form:
 *
 * <ul>
 * <li>Lua values of type "string" will be represented by Object type String.</li>
 * <li>Lua values of type "number" will be represented by Object type Double.</li>
 * <li>Lua values of type "boolean" will be represented by Object type Boolean.</li>
 * <li>Lua values of type "table" will be represented by Object type Map.</li>
 * <li>Lua values of any other type will be represented by a null object.</li>
 * </ul>
 *
 * This array will be empty if no arguments are passed. It is recommended you use {@link ArgumentHelper} in order to
 * validate and process arguments.
 *
 * This function may return {@link MethodResult}. However, if you simply return a value (rather than having to yield),
 * you may return {@code void}, a single value (either an object or a primitive like {@code int}) or array of objects.
 * These will be treated the same as {@link MethodResult#of()}, {@link MethodResult#of(Object)} and
 * {@link MethodResult#of(Object...)}.
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface LuaFunction
{
    /**
     * Explicitly specify the method names of this function. If not given, it uses the name of the annotated method.
     *
     * @return This function's name(s).
     */
    String[] value() default {};

    /**
     * Run this function on the main server thread. This should be specified for any method which interacts with
     * Minecraft in a thread-unsafe manner.
     *
     * @return Whether this functi
     * @see ILuaContext#issueMainThreadTask(ILuaTask)
     */
    boolean mainThread() default false;
}
