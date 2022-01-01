/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.lang.annotation.*;
import java.util.Map;
import java.util.Optional;

/**
 * Used to mark a Java function which is callable from Lua.
 *
 * Methods annotated with {@link LuaFunction} must be public final instance methods. They can have any number of
 * parameters, but they must be of the following types:
 *
 * <ul>
 *   <li>{@link ILuaContext} (and {@link IComputerAccess} if on a {@link IPeripheral})</li>
 *   <li>{@link IArguments}: The arguments supplied to this function.</li>
 *   <li>
 *     Alternatively, one may specify the desired arguments as normal parameters and the argument parsing code will
 *     be generated automatically.
 *
 *     Each parameter must be one of the given types supported by {@link IArguments} (for instance, {@link int} or
 *     {@link Map}). Optional values are supported by accepting a parameter of type {@link Optional}.
 *   </li>
 * </ul>
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
     * @return Whether this function should be run on the main thread.
     * @see ILuaContext#issueMainThreadTask(ILuaTask)
     */
    boolean mainThread() default false;

    /**
     * Allow using "unsafe" arguments, such {@link IArguments#getTableUnsafe(int)}.
     *
     * This is incompatible with {@link #mainThread()}.
     *
     * @return Whether this function supports unsafe arguments.
     */
    boolean unsafe() default false;
}
