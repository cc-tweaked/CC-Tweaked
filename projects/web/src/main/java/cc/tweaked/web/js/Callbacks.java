// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.TimerHandler;

/**
 * Invoke functions in the {@code $javaCallbacks} object. This global is set up by the Javascript code before the
 * Java code is started.
 * <p>
 * This module is a bit of a hack - we should be able to do most of this with module imports/exports. However, handling
 * those within TeaVM is a bit awkward, so this ends up being much easier.
 */
public class Callbacks {
    @JSFunctor
    @FunctionalInterface
    public interface AddComputer extends JSObject {
        ComputerHandle addComputer(ComputerDisplay computer);
    }

    /**
     * Export the {@link AddComputer} function to the Javascript code.
     *
     * @param addComputer The function to add a computer.
     */
    @JSBody(params = "setup", script = "return $javaCallbacks.setup(setup);")
    public static native void setup(AddComputer addComputer);

    /**
     * Get the version of CC: Tweaked.
     *
     * @return The mod's version.
     */
    @JSBody(script = "return $javaCallbacks.modVersion;")
    public static native String getModVersion();

    /**
     * List all resources available in the ROM.
     *
     * @return All available resources.
     */
    @JSBody(script = "return $javaCallbacks.listResources();")
    public static native String[] listResources();

    /**
     * Load a resource from the ROM.
     *
     * @param resource The path to the resource to load.
     * @return The loaded resource.
     */
    @JSByRef
    @JSBody(params = "name", script = "return $javaCallbacks.getResource(name);")
    public static native byte[] getResource(String resource);

    /**
     * Call {@code setImmediate} (or rather a polyfill) to run an asynchronous task.
     * <p>
     * While it would be nicer to use something built-in like {@code queueMicrotask}, our computer execution definitely
     * doesn't count as a microtask, and doing so will stall the UI thread.
     *
     * @param task The task to run.
     */
    @JSBody(params = "task", script = "return setImmediate(task);")
    public static native void setImmediate(TimerHandler task);
}
