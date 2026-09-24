// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core;

import com.google.errorprone.annotations.CheckReturnValue;
import dan200.computercraft.core.apis.http.HttpHandler;
import dan200.computercraft.core.apis.http.NettyHttp;
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
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The global context under which computers run.
 *
 * @param globalEnvironment   The global environment.
 * @param computerScheduler   The {@link ComputerScheduler} instance used to run computers. This is closed when the
 *                            context is closed, and so should be unique per-context.
 * @param mainThreadScheduler The {@link MainThreadScheduler} instance used to run main-thread tasks.
 * @param luaFactory          The factory to create new Lua machines.
 * @param luaMethods          The {@link MethodSupplier} used to find methods on Lua objects.
 * @param peripheralMethods   The {@link MethodSupplier} used to find methods on peripherals.
 * @param http                The {@link HttpHandler.Factory} used to create our HTTP implementation.
 */
public record ComputerContext(
    GlobalEnvironment globalEnvironment,
    ComputerScheduler computerScheduler,
    MainThreadScheduler mainThreadScheduler,
    ILuaMachine.Factory luaFactory,
    MethodSupplier<LuaMethod> luaMethods,
    MethodSupplier<PeripheralMethod> peripheralMethods,
    HttpHandler.Factory http
) {
    /**
     * Create a new {@link ComputerContext}.
     *
     * @deprecated Prefer using {@link Builder}.
     */
    @Deprecated
    public ComputerContext {
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
        private ILuaMachine.@Nullable Factory luaFactory;
        private @Nullable List<GenericMethod> genericMethods;
        private HttpHandler.@Nullable Factory http;

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
         * Disable HTTP for this {@link ComputerContext}.
         *
         * @return {@code this}, for chaining.
         * @see ComputerContext#http()
         */
        public Builder disableHttp() {
            return http(x -> null);
        }

        /**
         * Set the {@link HttpHandler.Factory} implementation. This creates a new {@link HttpHandler} for each computer.
         *
         * @param http The {@link HttpHandler} factory.
         * @return {@code this}, for chaining.
         */
        public Builder http(HttpHandler.Factory http) {
            Objects.requireNonNull(http);
            if (this.http != null) throw new IllegalStateException("HTTP provider already specified");
            this.http = http;
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
                PeripheralMethodSupplier.create(genericMethods == null ? List.of() : genericMethods),
                http == null ? new NettyHttp() : http
            );
        }
    }
}
