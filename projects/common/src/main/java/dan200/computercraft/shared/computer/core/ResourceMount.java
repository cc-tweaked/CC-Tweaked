// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.core.filesystem.ArchiveMount;
import dan200.computercraft.core.filesystem.FileSystem;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A mount backed by Minecraft's {@link ResourceManager}.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(MinecraftServer, String, String)
 */
public final class ResourceMount extends ArchiveMount<ResourceMount.FileEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMount.class);

    private static final byte[] TEMP_BUFFER = new byte[8192];

    /**
     * Maintain a cache of currently loaded resource mounts. This cache is invalidated when currentManager changes.
     */
    private static final Map<ResourceLocation, ResourceMount> MOUNT_CACHE = new HashMap<>(2);

    private final String namespace;
    private final String subPath;
    private ResourceManager manager;

    public static ResourceMount get(String namespace, String subPath, ResourceManager manager) {
        var path = new ResourceLocation(namespace, subPath);
        synchronized (MOUNT_CACHE) {
            var mount = MOUNT_CACHE.get(path);
            if (mount == null) MOUNT_CACHE.put(path, mount = new ResourceMount(namespace, subPath, manager));
            return mount;
        }
    }

    @VisibleForTesting
    ResourceMount(String namespace, String subPath, ResourceManager manager) {
        this.namespace = namespace;
        this.subPath = subPath;
        load(manager);
    }

    private void load(ResourceManager manager) {
        var hasAny = false;
        String existingNamespace = null;

        var newRoot = new FileEntry("", new ResourceLocation(namespace, subPath));
        for (var file : manager.listResources(subPath, s -> true).keySet()) {
            existingNamespace = file.getNamespace();

            if (!file.getNamespace().equals(namespace)) continue;
            if (!FileSystem.contains(subPath, file.getPath())) continue; // Some packs seem to include the parent?

            var localPath = FileSystem.toLocal(file.getPath(), subPath);
            create(newRoot, localPath);
            hasAny = true;
        }

        this.manager = manager;
        root = hasAny ? newRoot : null;

        if (!hasAny) {
            LOG.warn("Cannot find any files under /data/{}/{} for resource mount.", namespace, subPath);
            if (existingNamespace != null) {
                LOG.warn("There are files under /data/{}/{} though. Did you get the wrong namespace?", existingNamespace, subPath);
            }
        }
    }

    private void create(FileEntry lastEntry, String path) {
        var lastIndex = 0;
        while (lastIndex < path.length()) {
            var nextIndex = path.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = path.length();

            var part = path.substring(lastIndex, nextIndex);
            if (lastEntry.children == null) lastEntry.children = new HashMap<>();

            var nextEntry = lastEntry.children.get(part);
            if (nextEntry == null) {
                ResourceLocation childPath;
                try {
                    childPath = new ResourceLocation(namespace, subPath + "/" + path);
                } catch (ResourceLocationException e) {
                    LOG.warn("Cannot create resource location for {} ({})", part, e.getMessage());
                    return;
                }
                lastEntry.children.put(part, nextEntry = new FileEntry(path, childPath));
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }
    }

    @Override
    public long getSize(FileEntry file) {
        var resource = manager.getResource(file.identifier).orElse(null);
        if (resource == null) return 0;

        try (var stream = resource.open()) {
            int total = 0, read = 0;
            do {
                total += read;
                read = stream.read(TEMP_BUFFER);
            } while (read > 0);

            return total;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public byte[] getContents(FileEntry file) throws IOException {
        var resource = manager.getResource(file.identifier).orElse(null);
        if (resource == null) throw new FileOperationException(file.path, NO_SUCH_FILE);

        try (var stream = resource.open()) {
            return stream.readAllBytes();
        }
    }

    protected static class FileEntry extends ArchiveMount.FileEntry<FileEntry> {
        final ResourceLocation identifier;

        FileEntry(String path, ResourceLocation identifier) {
            super(path);
            this.identifier = identifier;
        }
    }

    /**
     * A {@link SimplePreparableReloadListener} which reloads any associated mounts and correctly updates the resource manager
     * they point to.
     */
    public static final SimplePreparableReloadListener<Void> RELOAD_LISTENER = new SimplePreparableReloadListener<>() {
        @Override
        protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
            profiler.push("Reloading ComputerCraft mounts");
            try {
                for (var mount : MOUNT_CACHE.values()) mount.load(manager);
            } finally {
                profiler.pop();
            }
            return null;
        }

        @Override
        protected void apply(Void result, ResourceManager manager, ProfilerFiller profiler) {
        }
    };
}
