// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import com.google.common.base.Splitter;
import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.WritableFileMount;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.MachineEnvironment;
import dan200.computercraft.core.lua.MachineException;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.computer.BasicEnvironment;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHook;
import org.squiddev.cobalt.debug.DebugState;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads tests from {@code test-rom/spec} and executes them.
 * <p>
 * This spins up a new computer and runs the {@code mcfly.lua} script. This will then load all files in the {@code spec}
 * directory and register them with {@code cct_test.start}.
 * <p>
 * From the test names, we generate a tree of {@link DynamicNode}s which queue an event and wait for
 * {@code cct_test.submit} to be called. McFly pulls these events, executes the tests and then calls the submit method.
 * <p>
 * Once all tests are done, we invoke {@code cct_test.finish} in order to mark everything as complete.
 */
public class ComputerTestDelegate {
    private static final Path REPORT_PATH = TestFiles.get("luacov.report.out");

    private static final Logger LOG = LoggerFactory.getLogger(ComputerTestDelegate.class);

    private static final long TICK_TIME = TimeUnit.MILLISECONDS.toNanos(50);

    private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(10);

    private static final Set<String> SKIP_KEYWORDS = new HashSet<>(
        Arrays.asList(System.getProperty("cc.skip_keywords", "").split(","))
    );

    private static final Pattern KEYWORD = Pattern.compile(":([a-z_]+)");

    private final ReentrantLock lock = new ReentrantLock();
    private ComputerContext context;
    private Computer computer;

    private final Condition hasTests = lock.newCondition();
    private DynamicNodeBuilder tests;

    private final Condition hasRun = lock.newCondition();
    private String currentTest;
    private boolean runFinished;
    private Throwable runResult;

    private final Condition hasFinished = lock.newCondition();
    private boolean finished = false;
    private final Map<LuaString, Int2IntArrayMap> coverage = new HashMap<>();

    @BeforeEach
    public void before() throws IOException {
        if (Files.deleteIfExists(REPORT_PATH)) LOG.info("Deleted previous coverage report.");

        var term = new Terminal(80, 100, true);
        WritableMount mount = new WritableFileMount(TestFiles.get("mount").toFile(), 10_000_000);

        // Remove any existing files
        List<String> children = new ArrayList<>();
        mount.list("", children);
        for (var child : children) mount.delete(child);

        // And add our startup file
        try (var channel = mount.openFile("startup.lua", MountConstants.WRITE_OPTIONS);
             var writer = Channels.newWriter(channel, StandardCharsets.UTF_8.newEncoder(), -1)) {
            writer.write("loadfile('test-rom/mcfly.lua', nil, _ENV)('test-rom/spec') cct_test.finish()");
        }

        var environment = new BasicEnvironment(mount);
        context = ComputerContext.builder(environment).luaFactory(CoverageLuaMachine::new).build();
        computer = new Computer(context, environment, term, 0);
        computer.getEnvironment().setPeripheral(ComputerSide.TOP, new FakeModem());
        computer.getEnvironment().setPeripheral(ComputerSide.BOTTOM, new FakePeripheralHub());
        computer.addApi(new CctTestAPI());

        computer.turnOn();
    }

    @AfterEach
    public void after() throws InterruptedException, IOException {
        try {
            LOG.info("Finished execution");
            computer.queueEvent("cct_test_run", null);

            // Wait for test execution to fully finish
            lock.lockInterruptibly();
            try {
                var remaining = TIMEOUT;
                while (remaining > 0 && !finished) {
                    tick();
                    if (hasFinished.awaitNanos(TICK_TIME) > 0) break;
                    remaining -= TICK_TIME;
                }

                if (remaining <= 0) throw new IllegalStateException("Timed out waiting for finish." + dump());
                if (!finished) throw new IllegalStateException("Computer did not finish." + dump());
            } finally {
                lock.unlock();
            }
        } finally {
            // Show a dump of computer output
            System.out.println(dump());

            // And shutdown
            computer.shutdown();
        }

        if (!coverage.isEmpty()) {
            Files.createDirectories(REPORT_PATH.getParent());
            try (var writer = Files.newBufferedWriter(REPORT_PATH)) {
                new LuaCoverage(coverage.entrySet().stream().collect(Collectors.toMap(
                    x -> x.getKey().substring(1).toString(), Map.Entry::getValue
                ))).write(writer);
            }
        }
    }

    @TestFactory
    public Stream<DynamicNode> get() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            var remaining = TIMEOUT;
            while (remaining > 0 && tests == null) {
                tick();
                if (hasTests.awaitNanos(TICK_TIME) > 0) break;
                remaining -= TICK_TIME;
            }

            if (remaining <= 0) throw new IllegalStateException("Timed out waiting for tests. " + dump());
            if (tests == null) throw new IllegalStateException("Computer did not provide any tests. " + dump());
        } finally {
            lock.unlock();
        }

        return tests.buildChildren();
    }

    private static class DynamicNodeBuilder {
        private final String name;
        private final URI uri;
        private final Map<String, DynamicNodeBuilder> children;
        private final Executable executor;

        DynamicNodeBuilder(String name, String path) {
            this.name = name;
            this.uri = getUri(path);
            this.children = new HashMap<>();
            this.executor = null;
        }

        DynamicNodeBuilder(String name, String path, Executable executor) {
            this.name = name;
            this.uri = getUri(path);
            this.children = Map.of();
            this.executor = executor;
        }

        private static URI getUri(String path) {
            // Unfortunately ?line=xxx doesn't appear to work with IntelliJ, so don't worry about getting it working.
            return path == null ? null : new File("src/test/resources" + path.substring(0, path.indexOf(':'))).toURI();
        }

        DynamicNodeBuilder get(String name) {
            var child = children.get(name);
            if (child == null) children.put(name, child = new DynamicNodeBuilder(name, null));
            return child;
        }

        void runs(String name, String uri, Executable executor) {
            if (this.executor != null) throw new IllegalStateException(name + " is leaf node");
            if (children.containsKey(name)) {
                var i = 1;
                while (children.containsKey(name + i)) i++;
                name = name + i;
            }

            children.put(name, new DynamicNodeBuilder(name, uri, executor));
        }

        boolean isActive() {
            var matcher = KEYWORD.matcher(name);
            while (matcher.find()) {
                if (SKIP_KEYWORDS.contains(matcher.group(1))) return false;
            }

            return true;
        }

        DynamicNode build() {
            return executor == null
                ? DynamicContainer.dynamicContainer(name, uri, buildChildren())
                : DynamicTest.dynamicTest(name, uri, executor);
        }

        Stream<DynamicNode> buildChildren() {
            return children.values().stream()
                .filter(DynamicNodeBuilder::isActive)
                .map(DynamicNodeBuilder::build);
        }
    }

    private String dump() {
        if (!computer.isOn()) return "Computer is currently off.";

        var term = computer.getAPIEnvironment().getTerminal();
        var builder = new StringBuilder().append("Computer is currently on.\n");

        for (var line = 0; line < term.getHeight(); line++) {
            builder.append(String.format("%2d | %" + term.getWidth() + "s |\n", line + 1, term.getLine(line)));
        }

        computer.shutdown();
        return builder.toString();
    }

    private void tick() {
        computer.tick();
    }

    private static String formatName(String name) {
        return name.replace("\0", " -> ");
    }

    public static class FakeModem implements IPeripheral {
        @Override
        public String getType() {
            return "modem";
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other;
        }

        @LuaFunction
        public final boolean isOpen(int channel) {
            return false;
        }
    }

    public static class FakePeripheralHub implements IPeripheral {
        @Override
        public String getType() {
            return "peripheral_hub";
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other;
        }

        @LuaFunction
        public final Collection<String> getNamesRemote() {
            return List.of("remote_1");
        }

        @LuaFunction
        public final boolean isPresentRemote(String name) {
            return name.equals("remote_1");
        }

        @LuaFunction
        public final Object[] getTypeRemote(String name) {
            return name.equals("remote_1") ? new Object[]{ "remote", "other_type" } : null;
        }

        @LuaFunction
        public final Object[] hasTypeRemote(String name, String type) {
            return name.equals("remote_1") ? new Object[]{ type.equals("remote") || type.equals("other_type") } : null;
        }

        @LuaFunction
        public final Object[] getMethodsRemote(String name) {
            return name.equals("remote_1") ? new Object[]{ List.of("func") } : null;
        }
    }

    public class CctTestAPI implements ILuaAPI {
        @Override
        public String[] getNames() {
            return new String[]{ "cct_test" };
        }

        @Override
        public void startup() {
            try {
                computer.getAPIEnvironment().getFileSystem().mount(
                    "test-rom", "test-rom",
                    BasicEnvironment.createMount(ComputerTestDelegate.class, "test-rom", "test")
                );
            } catch (FileSystemException e) {
                throw new IllegalStateException(e);
            }
        }

        @LuaFunction
        public final void start(Map<?, ?> tests) throws LuaException {
            // Submit several tests and signal for #get to run
            LOG.info("Received tests from computer");
            var root = new DynamicNodeBuilder("", null);
            for (Map.Entry<?, ?> entry : tests.entrySet()) {
                var name = (String) entry.getKey();
                var details = (Map<?, ?>) entry.getValue();
                var def = (String) details.get("definition");

                var parts = Splitter.on('\0').splitToList(name);
                var builder = root;
                for (var i = 0; i < parts.size() - 1; i++) builder = builder.get(parts.get(i));
                builder.runs(parts.get(parts.size() - 1), def, () -> {
                    // Run it
                    lock.lockInterruptibly();
                    try {
                        // Set the current test
                        runResult = null;
                        runFinished = false;
                        currentTest = name;

                        // Tell the computer to run it
                        LOG.info("Starting '{}'", formatName(name));
                        computer.queueEvent("cct_test_run", new Object[]{ name });

                        var remaining = TIMEOUT;
                        while (remaining > 0 && computer.isOn() && !runFinished) {
                            tick();

                            var waiting = hasRun.awaitNanos(TICK_TIME);
                            if (waiting > 0) break;
                            remaining -= TICK_TIME;
                        }

                        LOG.info("Finished '{}'", formatName(name));

                        if (remaining <= 0) {
                            throw new IllegalStateException("Timed out waiting for test");
                        } else if (!computer.isOn()) {
                            throw new IllegalStateException("Computer turned off mid-execution");
                        }

                        if (runResult != null) throw runResult;
                    } finally {
                        lock.unlock();
                        currentTest = null;
                    }
                });
            }

            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                ComputerTestDelegate.this.tests = root;
                hasTests.signal();
            } finally {
                lock.unlock();
            }
        }

        @LuaFunction
        public final void submit(Map<?, ?> tbl) {
            //  Submit the result of a test, allowing the test executor to continue
            var name = (String) tbl.get("name");
            var status = (String) tbl.get("status");
            var message = (String) tbl.get("message");
            var trace = (String) tbl.get("trace");

            var wholeMessage = new StringBuilder();
            if (message != null) wholeMessage.append(message);
            if (trace != null) {
                if (wholeMessage.length() != 0) wholeMessage.append('\n');
                wholeMessage.append(trace);
            }

            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                LOG.info("'{}' finished with {}", formatName(name), status);

                // Skip if a test mismatch
                if (!name.equals(currentTest)) {
                    LOG.warn("Skipping test '{}', as we're currently executing '{}'", formatName(name), formatName(currentTest));
                    return;
                }

                switch (status) {
                    case "ok":
                        break;
                    case "pending":
                        runResult = new TestAbortedException("Test is pending");
                        break;
                    case "fail":
                        runResult = new AssertionFailedError(wholeMessage.toString());
                        break;
                    case "error":
                        runResult = new IllegalStateException(wholeMessage.toString());
                        break;
                }

                runFinished = true;
                hasRun.signal();
            } finally {
                lock.unlock();
            }
        }

        @LuaFunction
        public final void finish() {
            LOG.info("Finished");

            // Signal to after that execution has finished
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                finished = true;

                hasFinished.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * A subclass of {@link CobaltLuaMachine} which tracks coverage for executed files.
     * <p>
     * This is a super nasty hack, but is also an order of magnitude faster than tracking this in Lua.
     */
    private class CoverageLuaMachine extends CobaltLuaMachine {
        CoverageLuaMachine(MachineEnvironment environment, InputStream bios) throws MachineException, IOException {
            super(environment, bios);

            LuaThread mainRoutine;
            try {
                var threadField = CobaltLuaMachine.class.getDeclaredField("mainRoutine");
                threadField.setAccessible(true);
                mainRoutine = (LuaThread) threadField.get(this);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Cannot get internal Cobalt state", e);
            }

            var coverage = ComputerTestDelegate.this.coverage;
            var hook = new DebugHook() {
                @Override
                public void onCall(LuaState state, DebugState ds, DebugFrame frame) {
                }

                @Override
                public void onReturn(LuaState state, DebugState ds, DebugFrame frame) {
                }

                @Override
                public void onCount(LuaState state, DebugState ds, DebugFrame frame) {
                }

                @Override
                public void onLine(LuaState state, DebugState ds, DebugFrame frame, int newLine) {
                    if (frame.closure == null) return;

                    var proto = frame.closure.getPrototype();
                    if (!proto.source.startsWith((byte) '@')) return;

                    var map = coverage.computeIfAbsent(proto.source, x -> new Int2IntArrayMap());
                    map.put(newLine, map.get(newLine) + 1);
                }
            };

            mainRoutine.getDebugState().setHook(hook, false, true, false, 0);
        }
    }
}
