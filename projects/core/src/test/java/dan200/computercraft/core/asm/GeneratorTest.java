// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.NamedMethod;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static dan200.computercraft.test.core.ContramapMatcher.contramap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GeneratorTest {
    private static final Generator<LuaMethod> GENERATOR = LuaMethodSupplier.GENERATOR;

    @Test
    public void testBasic() {
        var methods = GENERATOR.getMethods(Basic.class);
        assertThat(methods, contains(
            allOf(
                named("go"),
                contramap(is(true), "non-yielding", NamedMethod::nonYielding)
            )
        ));
    }

    @Test
    public void testIdentical() {
        var methods = GENERATOR.getMethods(Basic.class);
        var methods2 = GENERATOR.getMethods(Basic.class);
        assertThat(methods, sameInstance(methods2));
    }

    @Test
    public void testIdenticalMethods() {
        var methods = GENERATOR.getMethods(Basic.class);
        var methods2 = GENERATOR.getMethods(Basic2.class);
        assertThat(methods, contains(named("go")));
        assertThat(methods.get(0).method(), sameInstance(methods2.get(0).method()));
    }

    @Test
    public void testEmptyClass() {
        assertThat(GENERATOR.getMethods(Empty.class), is(empty()));
    }

    @Test
    public void testNonPublicClass() {
        assertThat(GENERATOR.getMethods(NonPublic.class), is(empty()));
    }

    @Test
    public void testNonInstance() {
        assertThat(GENERATOR.getMethods(NonInstance.class), is(empty()));
    }

    @Test
    public void testIllegalThrows() {
        assertThat(GENERATOR.getMethods(IllegalThrows.class), is(empty()));
    }

    @Test
    public void testCustomNames() {
        var methods = GENERATOR.getMethods(CustomNames.class);
        assertThat(methods, contains(named("go1"), named("go2")));
    }

    @Test
    public void testArgKinds() {
        var methods = GENERATOR.getMethods(ArgKinds.class);
        assertThat(methods, containsInAnyOrder(
            named("objectArg"), named("intArg"), named("optIntArg"),
            named("context"), named("arguments")
        ));
    }

    @Test
    public void testEnum() throws LuaException {
        var methods = GENERATOR.getMethods(EnumMethods.class);
        assertThat(methods, containsInAnyOrder(named("getEnum"), named("optEnum")));

        assertThat(apply(methods, new EnumMethods(), "getEnum", "front"), one(is("FRONT")));
        assertThat(apply(methods, new EnumMethods(), "optEnum", "front"), one(is("FRONT")));
        assertThat(apply(methods, new EnumMethods(), "optEnum"), one(is("?")));
        assertThrows(LuaException.class, () -> apply(methods, new EnumMethods(), "getEnum", "not as side"));
    }

    @Test
    public void testMainThread() throws LuaException {
        var methods = GENERATOR.getMethods(MainThread.class);
        assertThat(methods, contains(allOf(
            named("go"),
            contramap(is(false), "non-yielding", NamedMethod::nonYielding)
        )));

        assertThat(apply(methods, new MainThread(), "go"),
            contramap(notNullValue(), "callback", MethodResult::getCallback));
    }

    @Test
    public void testUnsafe() {
        var methods = GENERATOR.getMethods(Unsafe.class);
        assertThat(methods, contains(named("withUnsafe")));
    }

    public static class Basic {
        @LuaFunction
        public final void go() {
        }
    }

    public static class Basic2 extends Basic {
    }

    public static class Empty {
    }

    static class NonPublic {
        @LuaFunction
        public final void go() {
        }
    }

    public static class NonInstance {
        @LuaFunction
        public static void go() {
        }
    }

    public static class IllegalThrows {
        @LuaFunction
        @SuppressWarnings("DoNotCallSuggester")
        public final void go() throws IOException {
            throw new IOException();
        }
    }

    public static class CustomNames {
        @LuaFunction({ "go1", "go2" })
        public final void go() {
        }
    }

    public static class ArgKinds {
        @LuaFunction
        public final void objectArg(Object arg) {
        }

        @LuaFunction
        public final void intArg(int arg) {
        }

        @LuaFunction
        public final void optIntArg(Optional<Integer> arg) {
        }

        @LuaFunction
        public final void context(ILuaContext arg) {
        }

        @LuaFunction
        public final void arguments(IArguments arg) {
        }

        @LuaFunction
        public final void unknown(IComputerAccess arg) {
        }

        @LuaFunction
        public final void illegalMap(Map<String, Integer> arg) {
        }

        @LuaFunction
        public final void optIllegalMap(Optional<Map<String, Integer>> arg) {
        }
    }

    public static class EnumMethods {
        @LuaFunction
        public final String getEnum(ComputerSide side) {
            return side.name();
        }

        @LuaFunction
        public final String optEnum(Optional<ComputerSide> side) {
            return side.map(ComputerSide::name).orElse("?");
        }
    }

    public static class MainThread {
        @LuaFunction(mainThread = true)
        public final void go() {
        }
    }

    public static class Unsafe {
        @LuaFunction(unsafe = true)
        public final void withUnsafe(LuaTable<?, ?> table) {
        }

        @LuaFunction
        public final void withoutUnsafe(LuaTable<?, ?> table) {
        }

        @LuaFunction(unsafe = true, mainThread = true)
        public final void invalid(LuaTable<?, ?> table) {
        }
    }

    private static <T> T find(Collection<NamedMethod<T>> methods, String name) {
        return methods.stream()
            .filter(x -> x.name().equals(name))
            .map(NamedMethod::method)
            .findAny()
            .orElseThrow(NullPointerException::new);
    }

    public static MethodResult apply(Collection<NamedMethod<LuaMethod>> methods, Object instance, String name, Object... args) throws LuaException {
        return find(methods, name).apply(instance, CONTEXT, new ObjectArguments(args));
    }

    public static Matcher<MethodResult> one(Matcher<Object> object) {
        return allOf(
            contramap(nullValue(), "callback", MethodResult::getCallback),
            contramap(array(object), "result", MethodResult::getResult)
        );
    }

    public static <T> Matcher<NamedMethod<T>> named(String method) {
        return contramap(is(method), "name", NamedMethod::name);
    }

    private static final ILuaContext CONTEXT = new ILuaContext() {
        @Override
        public long issueMainThreadTask(LuaTask task) {
            return 0;
        }
    };
}
