// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.core.computer.ComputerThread;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.mainthread.MainThreadScheduler;
import dan200.computercraft.core.computer.mainthread.NoWorkMainThreadScheduler;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;

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
    private final ComputerThread computerScheduler;
    private final MainThreadScheduler mainThreadScheduler;
    private final ILuaMachine.Factory luaFactory;
    private final List<ILuaAPIFactory> apiFactories;

    ComputerContext(
        GlobalEnvironment globalEnvironment, ComputerThread computerScheduler,
        MainThreadScheduler mainThreadScheduler, ILuaMachine.Factory luaFactory,
        List<ILuaAPIFactory> apiFactories
    ) {
        this.globalEnvironment = globalEnvironment;
        this.computerScheduler = computerScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        this.luaFactory = luaFactory;
        this.apiFactories = apiFactories;
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
    public ComputerThread computerScheduler() {
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
     * Additional APIs to inject into each computer.
     *
     * @return All available API factories.
     */
    public List<ILuaAPIFactory> apiFactories() {
        return apiFactories;
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
        private int threads = 1;
        private @Nullable MainThreadScheduler mainThreadScheduler;
        private @Nullable ILuaMachine.Factory luaFactory;
        private @Nullable List<ILuaAPIFactory> apiFactories;

        Builder(GlobalEnvironment environment) {
            this.environment = environment;
        }

        /**
         * Set the number of threads the {@link ComputerThread} will use.
         *
         * @param threads The number of threads to use.
         * @return {@code this}, for chaining
         * @see ComputerContext#computerScheduler()
         */
        public Builder computerThreads(int threads) {
            if (threads < 1) throw new IllegalArgumentException("Threads must be >= 1");
            this.threads = threads;
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
         * Set the additional {@linkplain ILuaAPIFactory APIs} to add to each computer.
         *
         * @param apis A list of API factories.
         * @return {@code this}, for chaining
         * @see ComputerContext#apiFactories()
         */
        public Builder apiFactories(Collection<ILuaAPIFactory> apis) {
            Objects.requireNonNull(apis);
            if (apiFactories != null) throw new IllegalStateException("Main-thread scheduler already specified");
            apiFactories = List.copyOf(apis);
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
                new ComputerThread(threads),
                mainThreadScheduler == null ? new NoWorkMainThreadScheduler() : mainThreadScheduler,
                luaFactory == null ? CobaltLuaMachine::new : luaFactory,
                apiFactories == null ? List.of() : apiFactories
            );
        }
    }
}
