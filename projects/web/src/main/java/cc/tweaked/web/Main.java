// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import cc.tweaked.web.js.Callbacks;
import dan200.computercraft.core.ComputerContext;
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
        var context = ComputerContext.builder(EmulatorEnvironment.INSTANCE).build();
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
