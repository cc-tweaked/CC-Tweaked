// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import dan200.computercraft.core.asm.GenericMethod;
import dan200.computercraft.core.asm.LuaMethodSupplier;
import dan200.computercraft.core.asm.PeripheralMethodSupplier;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.computerthread.ComputerScheduler;
import dan200.computercraft.core.computer.computerthread.ComputerThread;
import dan200.computercraft.core.computer.mainthread.MainThreadScheduler;
import dan200.computercraft.core.computer.mainthread.NoWorkMainThreadScheduler;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineEnvironment;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The global context under which computers run.
 */
public final class ComputerContext {
    private final GlobalEnvironment globalEnvironment;
    private final ComputerScheduler computerScheduler;
    private final MainThreadScheduler mainThreadScheduler;
    private final ILuaMachine.Factory luaFactory;
    private final MethodSupplier<LuaMethod> luaMethods;
    private final MethodSupplier<PeripheralMethod> peripheralMethods;

    private ComputerContext(
        GlobalEnvironment globalEnvironment, ComputerScheduler computerScheduler,
        MainThreadScheduler mainThreadScheduler, ILuaMachine.Factory luaFactory,
        MethodSupplier<LuaMethod> luaMethods,
        MethodSupplier<PeripheralMethod> peripheralMethods
    ) {
        this.globalEnvironment = globalEnvironment;
        this.computerScheduler = computerScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        this.luaFactory = luaFactory;
        this.luaMethods = luaMethods;
        this.peripheralMethods = peripheralMethods;
    }

    /**
     * The global environment.
     *
     * @return The current global environment.
     */
    public GlobalEnvironment globalEnvironment() {
        return globalEnvironment;
    }

    /**
     * The {@link ComputerThread} instance under which computers are run. This is closed when the context is closed, and
     * so should be unique per-context.
     *
     * @return The current computer thread manager.
     */
    public ComputerScheduler computerScheduler() {
        return computerScheduler;
    }

    /**
     * The {@link MainThreadScheduler} instance used to run main-thread tasks.
     *
     * @return The current main thread scheduler.
     */
    public MainThreadScheduler mainThreadScheduler() {
        return mainThreadScheduler;
    }

    /**
     * The factory to create new Lua machines.
     *
     * @return The current Lua machine factory.
     */
    public ILuaMachine.Factory luaFactory() {
        return luaFactory;
    }

    /**
     * Get the {@link MethodSupplier} used to find methods on Lua values.
     *
     * @return The {@link LuaMethod} method supplier.
     * @see MachineEnvironment#luaMethods()
     */
    public MethodSupplier<LuaMethod> luaMethods() {
        return luaMethods;
    }

    /**
     * Get the {@link MethodSupplier} used to find methods on peripherals.
     *
     * @return The {@link PeripheralMethod} method supplier.
     */
    public MethodSupplier<PeripheralMethod> peripheralMethods() {
        return peripheralMethods;
    }

    /**
     * Close the current {@link ComputerContext}, disposing of any resources inside.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The unit {@code timeout} is in.
     * @return Whether the context was successfully shut down.
     * @throws InterruptedException If interrupted while waiting.
     */
    @CheckReturnValue
    public boolean close(long timeout, TimeUnit unit) throws InterruptedException {
        return computerScheduler().stop(timeout, unit);
    }

    /**
     * Close the current {@link ComputerContext}, disposing of any resources inside.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The unit {@code timeout} is in.
     * @throws IllegalStateException If the computer thread was not shut down in time.
     * @throws InterruptedException  If interrupted while waiting.
     */
    public void ensureClosed(long timeout, TimeUnit unit) throws InterruptedException {
        if (!computerScheduler().stop(timeout, unit)) {
            throw new IllegalStateException("Failed to shutdown ComputerContext in time.");
        }
    }

    /**
     * Create a new {@linkplain Builder builder} for a computer context.
     *
     * @param environment The {@linkplain ComputerContext#globalEnvironment() global environment} for this context.
     * @return The builder for a new context.
     */
    public static Builder builder(GlobalEnvironment environment) {
        return new Builder(environment);
    }

    /**
     * A builder for a {@link ComputerContext}.
     *
     * @see ComputerContext#builder(GlobalEnvironment)
     */
    public static class Builder {
        private final GlobalEnvironment environment;
        private @Nullable ComputerScheduler computerScheduler = null;
        private @Nullable MainThreadScheduler mainThreadScheduler;
        private @Nullable ILuaMachine.Factory luaFactory;
        private @Nullable List<GenericMethod> genericMethods;

        Builder(GlobalEnvironment environment) {
            this.environment = environment;
        }

        /**
         * Set the {@link #computerScheduler()} to use {@link ComputerThread} with a given number of threads.
         *
         * @param threads The number of threads to use.
         * @return {@code this}, for chaining
         * @see ComputerContext#computerScheduler()
         */
        public Builder computerThreads(int threads) {
            if (threads < 1) throw new IllegalArgumentException("Threads must be >= 1");
            return computerScheduler(new ComputerThread(threads));
        }

        /**
         * Set the {@link ComputerScheduler} for this context.
         *
         * @param scheduler The computer thread scheduler.
         * @return {@code this}, for chaining
         * @see ComputerContext#mainThreadScheduler()
         */
        public Builder computerScheduler(ComputerScheduler scheduler) {
            Objects.requireNonNull(scheduler);
            if (computerScheduler != null) throw new IllegalStateException("Computer scheduler already specified");
            computerScheduler = scheduler;
            return this;
        }

        /**
         * Set the {@link MainThreadScheduler} for this context.
         *
         * @param scheduler The main thread scheduler.
         * @return {@code this}, for chaining
         * @see ComputerContext#mainThreadScheduler()
         */
        public Builder mainThreadScheduler(MainThreadScheduler scheduler) {
            Objects.requireNonNull(scheduler);
            if (mainThreadScheduler != null) throw new IllegalStateException("Main-thread scheduler already specified");
            mainThreadScheduler = scheduler;
            return this;
        }

        /**
         * Set the {@link ILuaMachine.Factory} for this context.
         *
         * @param factory The Lua machine factory.
         * @return {@code this}, for chaining
         * @see ComputerContext#luaFactory()
         */
        public Builder luaFactory(ILuaMachine.Factory factory) {
            Objects.requireNonNull(factory);
            if (luaFactory != null) throw new IllegalStateException("Main-thread scheduler already specified");
            luaFactory = factory;
            return this;
        }

        /**
         * Set the set of {@link GenericMethod}s used by the {@linkplain MethodSupplier method suppliers}.
         *
         * @param genericMethods A list of API factories.
         * @return {@code this}, for chaining
         * @see ComputerContext#luaMethods()
         * @see ComputerContext#peripheralMethods()
         */
        public Builder genericMethods(Collection<GenericMethod> genericMethods) {
            Objects.requireNonNull(genericMethods);
            if (this.genericMethods != null) throw new IllegalStateException("Main-thread scheduler already specified");
            this.genericMethods = List.copyOf(genericMethods);
            return this;
        }

        /**
         * Create a new {@link ComputerContext}.
         *
         * @return The newly created context.
         */
        public ComputerContext build() {
            return new ComputerContext(
                environment,
                computerScheduler == null ? new ComputerThread(1) : computerScheduler,
                mainThreadScheduler == null ? new NoWorkMainThreadScheduler() : mainThreadScheduler,
                luaFactory == null ? CobaltLuaMachine::new : luaFactory,
                LuaMethodSupplier.create(genericMethods == null ? List.of() : genericMethods),
                PeripheralMethodSupplier.create(genericMethods == null ? List.of() : genericMethods)
            );
        }
    }
}
