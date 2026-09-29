package com.example.examplemod;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.component.ComputerComponents;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.turtle.ITurtleAccess;
import org.jspecify.annotations.Nullable;

/**
 * An example API that will be available on every turtle. This demonstrates both registering an API, and how to write
 * Lua-facing functions.
 * <p>
 * This API is not available as a global (as {@link #getNames() returns nothing}), but is instead accessible via
 * {@code require} (see {@link #getModuleName()}).
 *
 * <h2>Example</h2>
 * <pre class="language language-lua">{@code
 * local my_api = require("example.my_api")
 * print("Turtle is facing " .. my_api.getDirection())
 * }</pre>
 */
public class ExampleAPI implements ILuaAPI {
    private final ITurtleAccess turtle;

    public ExampleAPI(ITurtleAccess turtle) {
        this.turtle = turtle;
    }

    public static void register() {
        // @start region=register
        ComputerCraftAPI.registerAPIFactory(computer -> {
            // Read the turtle component.
            var turtle = computer.getComponent(ComputerComponents.TURTLE);
            // If present then add our API.
            return turtle == null ? null : new ExampleAPI(turtle);
        });
        // @end region=register
    }

    @Override
    public String[] getNames() {
        return new String[0];
    }

    @Override
    public @Nullable String getModuleName() {
        return "example.my_api";
    }

    /**
     * A Lua-facing function that returns the direction the turtle is facing.
     *
     * @return The turtle's direction.
     */
    @LuaFunction
    public final String getDirection() {
        return turtle.getDirection().getName();
    }

    /**
     * A Lua-facing function using {@link Coerced}. Unlike a {@link LuaFunction} taking a raw {@link String}, this will
     * accept any value, and convert it to a string.
     *
     * @param myString The value to write.
     */
    // @start region=coerced
    @LuaFunction
    public final void writeString(Coerced<String> myString) {
        String contents = myString.value();
        System.out.println("Got " + contents);
    }
    // @end region=coerced
}
