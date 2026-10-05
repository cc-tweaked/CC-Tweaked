// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import cc.tweaked.web.http.JsHttpHandler;
import cc.tweaked.web.js.Callbacks;
import cc.tweaked.web.methods.StaticLuaMethodSupplier;
import cc.tweaked.web.methods.StaticPeripheralMethodSupplier;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.mainthread.NoWorkMainThreadScheduler;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import org.teavm.jso.browser.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * The main entrypoint to the emulator.
 */
public class Main {
    public static final String CORS_PROXY = "https://copy-cat-cors.vercel.app/?{}";

    private static long ticks;

    public static void main(String[] args) {
        @SuppressWarnings("deprecation") // Intentional.
        var context = new ComputerContext(
            EmulatorEnvironment.INSTANCE,
            JsComputerScheduler.INSTANCE,
            new NoWorkMainThreadScheduler(),
            CobaltLuaMachine::new,
            StaticLuaMethodSupplier.INSTANCE,
            StaticPeripheralMethodSupplier.INSTANCE,
            JsHttpHandler::new
        );

        List<EmulatedComputer> computers = new ArrayList<>();

        Callbacks.setup(access -> {
            var wrapper = new EmulatedComputer(context, access);
            computers.add(wrapper);
            return wrapper;
        });

        Window.setInterval(() -> {
            ticks++;
            var iterator = computers.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().tick()) iterator.remove();
            }
        }, 50);
    }

    public static long getTicks() {
        return ticks;
    }
}
