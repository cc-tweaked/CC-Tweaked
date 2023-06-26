// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.apis.*;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineEnvironment;
import dan200.computercraft.core.lua.MachineException;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.core.util.Nullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The main task queue and executor for a single computer. This handles turning on and off a computer, as well as
 * running events.
 * <p>
 * When the computer is instructed to turn on or off, or handle an event, we queue a task and register this to be
 * executed on the {@link ComputerThread}. Note, as we may be starting many events in a single tick, the external
 * cannot lock on anything which may be held for a long time.
 * <p>
 * The executor is effectively composed of two separate queues. Firstly, we have a "single element" queue
 * {@link #command} which determines which state the computer should transition too. This is set by
 * {@link #queueStart()} and {@link #queueStop(boolean, boolean)}.
 * <p>
 * When a computer is on, we simply push any events onto to the {@link #eventQueue}.
 * <p>
 * Both queues are run from the {@link #work()} method, which tries to execute a command if one exists, or resumes the
 * machine with an event otherwise.
 * <p>
 * One final responsibility for the executor is calling {@link ILuaAPI#update()} every tick, via the {@link #tick()}
 * method. This should only be called when the computer is actually on ({@link #isOn}).
 */
final class ComputerExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerExecutor.class);
    private static final int QUEUE_LIMIT = 256;

    private final Computer computer;
    private final ComputerEnvironment computerEnvironment;
    private final MetricsObserver metrics;
    private final List<ILuaAPI> apis = new ArrayList<>();
    private final ComputerThread scheduler;
    final TimeoutState timeout;

    private @Nullable FileSystem fileSystem;

    private @Nullable ILuaMachine machine;

    /**
     * Whether the computer is currently on. This is set to false when a shutdown starts, or when turning on completes
     * (but just before the Lua machine is started).
     *
     * @see #isOnLock
     */
    private volatile boolean isOn = false;

    /**
     * The lock to acquire when you need to modify the "on state" of a computer.
     * <p>
     * We hold this lock when running any command, and attempt to hold it when updating APIs. This ensures you don't
     * update APIs while also starting/stopping them.
     *
     * @see #isOn
     * @see #tick()
     * @see #turnOn()
     * @see #shutdown()
     */
    private final ReentrantLock isOnLock = new ReentrantLock();

    /**
     * A lock used for any changes to {@link #eventQueue}, {@link #command} or {@link #onComputerQueue}. This will be
     * used on the main thread, so locks should be kept as brief as possible.
     */
    private final Object queueLock = new Object();

    /**
     * Determines if this executor is present within {@link ComputerThread}.
     *
     * @see #queueLock
     * @see #enqueue()
     * @see #afterWork()
     */
    volatile boolean onComputerQueue = false;

    /**
     * The amount of time this computer has used on a theoretical machine which shares work evenly amongst computers.
     *
     * @see ComputerThread
     */
    long virtualRuntime = 0;

    /**
     * The last time at which we updated {@link #virtualRuntime}.
     *
     * @see ComputerThread
     */
    long vRuntimeStart;

    /**
     * The command that {@link #work()} should execute on the computer thread.
     * <p>
     * One sets the command with {@link #queueStart()} and {@link #queueStop(boolean, boolean)}. Neither of these will
     * queue a new event if there is an existing one in the queue.
     * <p>
     * Note, if command is not {@code null}, then some command is scheduled to be executed. Otherwise it is not
     * currently in the queue (or is currently being executed).
     */
    private volatile @Nullable StateCommand command;

    /**
     * The queue of events which should be executed when this computer is on.
     * <p>
     * Note, this should be empty if this computer is off - it is cleared on shutdown and when turning on again.
     */
    private final Queue<Event> eventQueue = new ArrayDeque<>(4);

    /**
     * Whether we interrupted an event and so should resume it instead of executing another task.
     *
     * @see #work()
     * @see #resumeMachine(String, Object[])
     */
    private boolean interruptedEvent = false;

    /**
     * Whether this executor has been closed, and will no longer accept any incoming commands or events.
     *
     * @see #queueStop(boolean, boolean)
     */
    private boolean closed;

    private @Nullable WritableMount rootMount;

    /**
     * The thread the executor is running on. This is non-null when performing work. We use this to ensure we're only
     * doing one bit of work at one time.
     *
     * @see ComputerThread
     */
    final AtomicReference<Thread> executingThread = new AtomicReference<>();

    private final ILuaMachine.Factory luaFactory;

    ComputerExecutor(Computer computer, ComputerEnvironment computerEnvironment, ComputerContext context) {
        this.computer = computer;
        this.computerEnvironment = computerEnvironment;
        metrics = computerEnvironment.getMetrics();
        luaFactory = context.luaFactory();
        scheduler = context.computerScheduler();
        timeout = new TimeoutState(scheduler);

        var environment = computer.getEnvironment();

        // Add all default APIs to the loaded list.
        apis.add(new TermAPI(environment));
        apis.add(new RedstoneAPI(environment));
        apis.add(new FSAPI(environment));
        apis.add(new PeripheralAPI(environment));
        apis.add(new OSAPI(environment));
        if (CoreConfig.httpEnabled) apis.add(new HTTPAPI(environment));

        // Load in the externally registered APIs.
        for (var factory : context.apiFactories()) {
            var system = new ComputerSystem(environment);
            var api = factory.create(system);
            if (api != null) apis.add(new ApiWrapper(api, system));
        }
    }

    boolean isOn() {
        return isOn;
    }

    FileSystem getFileSystem() {
        var fileSystem = this.fileSystem;
        if (fileSystem == null) throw new IllegalStateException("FileSystem has not been created yet");
        return fileSystem;
    }

    Computer getComputer() {
        return computer;
    }

    void addApi(ILuaAPI api) {
        apis.add(api);
    }

    /**
     * Schedule this computer to be started if not already on.
     */
    void queueStart() {
        synchronized (queueLock) {
            // We should only schedule a start if we're not currently on and there's turn on.
            if (closed || isOn || command != null) return;

            command = StateCommand.TURN_ON;
            enqueue();
        }
    }

    /**
     * Schedule this computer to be stopped if not already on.
     *
     * @param reboot Reboot the computer after stopping
     * @param close  Close the computer after stopping.
     * @see #closed
     */
    void queueStop(boolean reboot, boolean close) {
        synchronized (queueLock) {
            if (closed) return;
            closed = close;

            var newCommand = reboot ? StateCommand.REBOOT : StateCommand.SHUTDOWN;

            // We should only schedule a stop if we're currently on and there's no shutdown pending.
            if (!isOn || command != null) {
                // If we're closing, set the command just in case.
                if (close) command = newCommand;
                return;
            }

            command = newCommand;
            enqueue();
        }
    }

    /**
     * Abort this whole computer due to a timeout. This will immediately destroy the Lua machine,
     * and then schedule a shutdown.
     */
    void abort() {
        immediateFail(StateCommand.ABORT);
    }

    /**
     * Abort this whole computer due to an internal error. This will immediately destroy the Lua machine,
     * and then schedule a shutdown.
     */
    void fastFail() {
        immediateFail(StateCommand.ERROR);
    }

    private void immediateFail(StateCommand command) {
        var machine = this.machine;
        if (machine != null) machine.close();

        synchronized (queueLock) {
            if (closed) return;
            this.command = command;
            if (isOn) enqueue();
        }
    }

    /**
     * Queue an event if the computer is on.
     *
     * @param event The event's name
     * @param args  The event's arguments
     */
    void queueEvent(String event, @Nullable Object[] args) {
        // Events should be skipped if we're not on.
        if (!isOn) return;

        synchronized (queueLock) {
            // And if we've got some command in the pipeline, then don't queue events - they'll
            // probably be disposed of anyway.
            // We also limit the number of events which can be queued.
            if (closed || command != null || eventQueue.size() >= QUEUE_LIMIT) return;

            eventQueue.offer(new Event(event, args));
            enqueue();
        }
    }

    /**
     * Add this executor to the {@link ComputerThread} if not already there.
     */
    private void enqueue() {
        synchronized (queueLock) {
            if (!onComputerQueue) scheduler.queue(this);
        }
    }

    /**
     * Update the internals of the executor.
     */
    void tick() {
        if (isOn && isOnLock.tryLock()) {
            // This horrific structure means we don't try to update APIs while the state is being changed
            // (and so they may be running startup/shutdown).
            // We use tryLock here, as it has minimal delay, and it doesn't matter if we miss an advance at the
            // beginning or end of a computer's lifetime.
            try {
                if (isOn) {
                    // Advance our APIs.
                    for (var api : apis) api.update();
                }
            } finally {
                isOnLock.unlock();
            }
        }
    }

    @Nullable
    private Mount getRomMount() {
        return computer.getGlobalEnvironment().createResourceMount("computercraft", "lua/rom");
    }

    @Nullable
    private WritableMount getRootMount() {
        if (rootMount == null) rootMount = computerEnvironment.createRootMount();
        return rootMount;
    }

    @Nullable
    private FileSystem createFileSystem() {
        FileSystem filesystem = null;
        try {
            var mount = getRootMount();
            if (mount == null) {
                displayFailure("Cannot mount computer mount", null);
                return null;
            }

            filesystem = new FileSystem("hdd", mount);

            var romMount = getRomMount();
            if (romMount == null) {
                displayFailure("Cannot mount ROM", null);
                return null;
            }

            filesystem.mount("rom", "rom", romMount);
            return filesystem;
        } catch (FileSystemException e) {
            if (filesystem != null) filesystem.close();
            LOG.error("Cannot mount computer filesystem", e);

            displayFailure("Cannot mount computer system", null);
            return null;
        }
    }

    @Nullable
    private ILuaMachine createLuaMachine() {
        // Load the bios resource
        InputStream biosStream = null;
        try {
            biosStream = computer.getGlobalEnvironment().createResourceFile("computercraft", "lua/bios.lua");
        } catch (Exception e) {
            LOG.error("Failed to load BIOS", e);
        }

        if (biosStream == null) {
            displayFailure("Error loading bios.lua", null);
            return null;
        }

        // Create the lua machine
        try (var bios = biosStream) {
            return luaFactory.create(new MachineEnvironment(
                new LuaContext(computer), metrics, timeout,
                () -> apis.stream().map(api -> api instanceof ApiWrapper wrapper ? wrapper.getDelegate() : api).iterator(),
                computer.getGlobalEnvironment().getHostString()
            ), bios);
        } catch (IOException e) {
            LOG.error("Failed to read bios.lua", e);
            displayFailure("Error loading bios.lua", null);
            return null;
        } catch (MachineException e) {
            displayFailure("Error loading bios.lua", e.getMessage());
            return null;
        }
    }

    private void turnOn() throws InterruptedException {
        isOnLock.lockInterruptibly();
        try {
            // Reset the terminal and event queue
            computer.getTerminal().reset();
            interruptedEvent = false;
            synchronized (queueLock) {
                eventQueue.clear();
            }

            // Init filesystem
            if ((fileSystem = createFileSystem()) == null) {
                shutdown();
                return;
            }

            // Init APIs
            computer.getEnvironment().reset();
            for (var api : apis) api.startup();

            // Init lua
            if ((machine = createLuaMachine()) == null) {
                shutdown();
                return;
            }

            // Initialisation has finished, so let's mark ourselves as on.
            isOn = true;
            computer.markChanged();
        } finally {
            isOnLock.unlock();
        }

        // Now actually start the computer, now that everything is set up.
        resumeMachine(null, null);
    }

    private void shutdown() throws InterruptedException {
        isOnLock.lockInterruptibly();
        try {
            isOn = false;
            interruptedEvent = false;
            synchronized (queueLock) {
                eventQueue.clear();
            }

            // Shutdown Lua machine
            if (machine != null) {
                machine.close();
                machine = null;
            }

            // Shutdown our APIs
            for (var api : apis) api.shutdown();
            computer.getEnvironment().reset();

            // Unload filesystem
            if (fileSystem != null) {
                fileSystem.close();
                fileSystem = null;
            }

            computer.getEnvironment().resetOutput();
            computer.markChanged();
        } finally {
            isOnLock.unlock();
        }
    }

    /**
     * Called before calling {@link #work()}, setting up any important state.
     */
    void beforeWork() {
        vRuntimeStart = System.nanoTime();
        timeout.startTimer();
    }

    /**
     * Called after executing {@link #work()}.
     *
     * @return If we have more work to do.
     */
    boolean afterWork() {
        if (interruptedEvent) {
            timeout.pauseTimer();
        } else {
            timeout.stopTimer();
        }

        metrics.observe(Metrics.COMPUTER_TASKS, timeout.nanoCurrent());

        if (interruptedEvent) return true;

        synchronized (queueLock) {
            if (eventQueue.isEmpty() && command == null) return onComputerQueue = false;
            return true;
        }
    }

    /**
     * The main worker function, called by {@link ComputerThread}.
     * <p>
     * This either executes a {@link StateCommand} or attempts to run an event
     *
     * @throws InterruptedException If various locks could not be acquired.
     * @see #command
     * @see #eventQueue
     */
    void work() throws InterruptedException {
        if (interruptedEvent && !closed) {
            interruptedEvent = false;
            if (machine != null) {
                resumeMachine(null, null);
                return;
            }
        }

        StateCommand command;
        Event event = null;
        synchronized (queueLock) {
            command = this.command;
            this.command = null;

            // If we've no command, pull something from the event queue instead.
            if (command == null) {
                if (!isOn) {
                    // We're not on and had no command, but we had work queued. This should never happen, so clear
                    // the event queue just in case.
                    eventQueue.clear();
                    return;
                }

                event = eventQueue.poll();
            }
        }

        if (command != null) {
            switch (command) {
                case TURN_ON -> {
                    if (isOn) return;
                    turnOn();
                }
                case SHUTDOWN -> {
                    if (!isOn) return;
                    computer.getTerminal().reset();
                    shutdown();
                }
                case REBOOT -> {
                    if (!isOn) return;
                    computer.getTerminal().reset();
                    shutdown();
                    computer.turnOn();
                }
                case ABORT -> {
                    if (!isOn) return;
                    displayFailure("Error running computer", TimeoutState.ABORT_MESSAGE);
                    shutdown();
                }
                case ERROR -> {
                    if (!isOn) return;
                    displayFailure("Error running computer", "An internal error occurred, see logs.");
                    shutdown();
                }
            }
        } else if (event != null) {
            resumeMachine(event.name, event.args);
        }
    }

    void printState(StringBuilder out) {
        out.append("Enqueued command: ").append(command).append('\n');
        out.append("Enqueued events: ").append(eventQueue.size()).append('\n');

        var machine = this.machine;
        if (machine != null) machine.printExecutionState(out);
    }

    private void displayFailure(String message, @Nullable String extra) {
        var terminal = computer.getTerminal();
        terminal.reset();

        // Display our primary error message
        if (terminal.isColour()) terminal.setTextColour(15 - Colour.RED.ordinal());
        terminal.write(message);

        if (extra != null) {
            // Display any additional information. This generally comes from the Lua Machine, such as compilation or
            // runtime errors.
            terminal.setCursorPos(0, terminal.getCursorY() + 1);
            terminal.write(extra);
        }

        // And display our generic "CC may be installed incorrectly" message.
        terminal.setCursorPos(0, terminal.getCursorY() + 1);
        if (terminal.isColour()) terminal.setTextColour(15 - Colour.WHITE.ordinal());
        terminal.write("ComputerCraft may be installed incorrectly");
    }

    private void resumeMachine(@Nullable String event, @Nullable Object[] args) throws InterruptedException {
        var result = Nullability.assertNonNull(machine).handleEvent(event, args);
        interruptedEvent = result.isPause();
        if (!result.isError()) return;

        displayFailure("Error running computer", result.getMessage());
        shutdown();
    }

    private enum StateCommand {
        TURN_ON,
        SHUTDOWN,
        REBOOT,
        ABORT,
        ERROR,
    }

    private record Event(String name, @Nullable Object[] args) {
    }
}
