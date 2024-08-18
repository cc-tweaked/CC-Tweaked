// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import com.google.common.io.CharStreams;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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

    public static void main(String[] args) throws Exception {
        var stream = ComputerTest.class.getClassLoader().getResourceAsStream("benchmark.lua");
        try (var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            var contents = CharStreams.toString(reader);
            ComputerBootstrap.run(contents, 1000);
        }
    }
}
