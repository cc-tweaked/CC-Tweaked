// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.network.PacketNetwork;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.mainthread.MainThread;
import dan200.computercraft.core.computer.mainthread.MainThreadConfig;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.impl.AbstractComputerCraftAPI;
import dan200.computercraft.impl.GenericSources;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.computer.metrics.GlobalMetrics;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.util.IDAssigner;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Stores ComputerCraft's server-side state for the lifetime of a {@link MinecraftServer}.
 * <p>
 * This is effectively a glorified singleton, holding references to most global state ComputerCraft stores
 * (for instance, {@linkplain IDAssigner ID assignment} and {@linkplain  ServerComputerRegistry running computers}. Its
 * main purpose is to offer a single point of resetting the state ({@link ServerContext#close()} and ensure disciplined
 * access to the current state, by ensuring callers have a {@link MinecraftServer} to hand.
 *
 * @see CommonHooks for where the context is created and torn down.
 */
public final class ServerContext {
    private static final Logger LOG = LoggerFactory.getLogger(ServerContext.class);

    private static final LevelResource FOLDER = new LevelResource(ComputerCraftAPI.MOD_ID);

    @VisibleForTesting
    public static ILuaMachine.Factory luaMachine = CobaltLuaMachine::new;

    private static @Nullable ServerContext instance;

    private final MinecraftServer server;

    private final ServerComputerRegistry registry = new ServerComputerRegistry();
    private final GlobalMetrics metrics = new GlobalMetrics();
    private final ComputerContext context;
    private final MainThread mainThread;
    private final IDAssigner idAssigner;
    private final WirelessNetwork wirelessNetwork = new WirelessNetwork();
    private final Path storageDir;

    private ServerContext(MinecraftServer server) {
        this.server = server;
        storageDir = server.getWorldPath(FOLDER);
        mainThread = new MainThread(mainThreadConfig);
        context = ComputerContext.builder(new Environment(server))
            .computerThreads(ConfigSpec.computerThreads.get())
            .mainThreadScheduler(mainThread)
            .luaFactory(luaMachine)
            .genericMethods(GenericSources.getAllMethods())
            .build();
        idAssigner = new IDAssigner(storageDir.resolve("ids.json"));
    }

    /**
     * Start a new server context from the currently running Minecraft server.
     *
     * @param server The currently running Minecraft server.
     * @throws IllegalStateException If a context is already present.
     */
    public static void create(MinecraftServer server) {
        if (ServerContext.instance != null) throw new IllegalStateException("ServerContext already exists!");
        ServerContext.instance = new ServerContext(server);
    }

    /**
     * Stops the current server context, resetting any state and terminating all computers.
     */
    public static void close() {
        var instance = ServerContext.instance;
        if (instance == null) return;

        instance.registry.close();
        try {
            if (!instance.context.close(1, TimeUnit.SECONDS)) {
                LOG.error("Failed to stop computers under deadline.");
            }
        } catch (InterruptedException e) {
            LOG.error("Failed to stop computers.", e);
            Thread.currentThread().interrupt();
        }

        ServerContext.instance = null;
    }

    /**
     * Get the {@link ServerContext} instance for the currently running Minecraft server.
     *
     * @param server The current server.
     * @return The current server context.
     */
    public static ServerContext get(MinecraftServer server) {
        Objects.requireNonNull(server, "Server cannot be null");

        var instance = ServerContext.instance;
        if (instance == null) throw new IllegalStateException("ServerContext has not been started yet");
        if (instance.server != server) {
            throw new IllegalStateException("Incorrect server given. Did ServerContext shutdown correctly?");
        }

        return instance;
    }

    /**
     * Get the current {@link ComputerContext} computers should run under.
     *
     * @return The current {@link ComputerContext}.
     */
    ComputerContext computerContext() {
        return context;
    }

    /**
     * Get the {@link MethodSupplier} used to find methods on peripherals.
     *
     * @return The {@link PeripheralMethod} method supplier.
     * @see ComputerContext#peripheralMethods()
     */
    public MethodSupplier<PeripheralMethod> peripheralMethods() {
        return context.peripheralMethods();
    }

    /**
     * Tick all components of this server context. This should <em>NOT</em> be called outside of {@link CommonHooks}.
     */
    public void tick() {
        registry.update();
        mainThread.tick();
    }

    /**
     * Get the current {@link ServerComputerRegistry}.
     *
     * @return The global computer registry.
     */
    public ServerComputerRegistry registry() {
        return registry;
    }

    /**
     * Return the next available ID for a particular kind (for instance, a computer or particular peripheral type).
     * <p>
     * IDs are assigned incrementally, with the last assigned ID being stored in {@code ids.json} in our root
     * {@linkplain    #storageDir() storage folder}.
     *
     * @param kind The kind we're assigning an ID for, for instance {@code "computer"} or {@code "peripheral.monitor"}.
     * @return The next available ID.
     * @see ComputerCraftAPI#createUniqueNumberedSaveDir(MinecraftServer, String)
     */
    public int getNextId(String kind) {
        return idAssigner.getNextId(kind);
    }

    /**
     * Get the directory used for all ComputerCraft related information. This includes the computer/peripheral id store,
     * and all computer data.
     *
     * @return The storge directory for ComputerCraft.
     */
    public Path storageDir() {
        return storageDir;
    }

    /**
     * Get the current global metrics store.
     *
     * @return The current metrics store.
     */
    public GlobalMetrics metrics() {
        return metrics;
    }

    /**
     * Get the global wireless network.
     * <p>
     * Use {@link ComputerCraftAPI#getWirelessNetwork(MinecraftServer)} instead of this method.
     *
     * @return The wireless network.
     */
    public PacketNetwork wirelessNetwork() {
        return wirelessNetwork;
    }

    private record Environment(MinecraftServer server) implements GlobalEnvironment {
        @Override
        public @Nullable Mount createResourceMount(String domain, String subPath) {
            return ComputerCraftAPI.createResourceMount(server, domain, subPath);
        }

        @Override
        public @Nullable InputStream createResourceFile(String domain, String subPath) {
            return AbstractComputerCraftAPI.getResourceFile(server, domain, subPath);
        }

        @Override
        public String getHostString() {
            var version = SharedConstants.getCurrentVersion().getName();
            return String.format("ComputerCraft %s (Minecraft %s)", ComputerCraftAPI.getInstalledVersion(), version);
        }

        @Override
        public String getUserAgent() {
            return ComputerCraftAPI.MOD_ID + "/" + ComputerCraftAPI.getInstalledVersion();
        }
    }

    private static final MainThreadConfig mainThreadConfig = new MainThreadConfig() {
        @Override
        public long maxGlobalTime() {
            return TimeUnit.MILLISECONDS.toNanos(ConfigSpec.maxMainGlobalTime.get());
        }

        @Override
        public long maxComputerTime() {
            return TimeUnit.MILLISECONDS.toNanos(ConfigSpec.maxMainComputerTime.get());
        }
    };
}
