// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import com.google.common.io.CharStreams;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.computer.BasicEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

public class ComputerTest {
    @Test
    public void testTimeout() {
        assertTimeoutPreemptively(ofSeconds(20), () -> {
            try {
                ComputerBootstrap.run("print('Hello') while true do end", ComputerBootstrap.MAX_TIME);
            } catch (AssertionError e) {
                if (e.getMessage().equals("/test.lua:1: Too long without yielding")) return;
                throw e;
            }

            Assertions.fail("Expected computer to timeout");
        });
    }

    @Test
    public void testDuplicateObjects() {
        class CustomApi implements ILuaAPI {
            @Override
            public String[] getNames() {
                return new String[]{ "custom" };
            }

            @LuaFunction
            public final Object[] getObjects() {
                return new Object[]{ List.of(), List.of() };
            }
        }

        ComputerBootstrap.run("""
            local x, y = custom.getObjects()
            assert(x ~= y)
            """, i -> i.addApi(new CustomApi()), 50);
    }

    @Test
    public void testTurnOnShutdown() throws InterruptedException {
        var env = new BasicEnvironment();
        var scheduler = new MockComputerScheduler();
        var context = ComputerContext.builder(env).computerScheduler(scheduler).luaFactory(MockLuaMachine.factory()).build();
        try {
            var computer = new Computer(context, env, new Terminal(51, 19, false), 0);

            // Schedule a turn on, and ensure ticking turns the computer on.
            computer.turnOn();
            computer.tick();
            scheduler.assertDoWork();
            assertTrue(computer.isOn(), "Computer should be on");

            // Schedule another turn on.
            computer.turnOn();
            computer.shutdown();
            scheduler.assertDoWork();
            assertFalse(computer.isOn(), "Computer should be off");

            for (var i = 0; i < 100; i++) computer.tick();
            assertFalse(scheduler.hasWork(), "Computer should not have scheduled any work");
            assertFalse(computer.isOn(), "Computer should still be off");
        } finally {
            context.ensureClosed(5, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws Exception {
        var stream = ComputerTest.class.getClassLoader().getResourceAsStream("benchmark.lua");
        try (var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            var contents = CharStreams.toString(reader);
            ComputerBootstrap.run(contents, 1000);
        }
    }
}
