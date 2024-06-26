// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.core.filesystem.ArchiveMount;
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

import static dan200.computercraft.api.filesystem.MountConstants.NO_SUCH_FILE;

/**
 * A mount backed by Minecraft's {@link ResourceManager}.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(MinecraftServer, String, String)
 */
public final class ResourceMount extends ArchiveMount<ResourceMount.FileEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMount.class);

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

        var newRoot = new FileEntry(new ResourceLocation(namespace, subPath));
        for (var file : manager.listResources(subPath, s -> true).keySet()) {
            existingNamespace = file.getNamespace();

            if (!file.getNamespace().equals(namespace)) continue;

            var localPath = getLocalPath(file.getPath(), subPath);
            if (localPath == null) continue;

            try {
                getOrCreateChild(newRoot, localPath, this::createEntry);
            } catch (ResourceLocationException e) {
                LOG.warn("Cannot create resource location for {} ({})", localPath, e.getMessage());
            }
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

    private FileEntry createEntry(String path) {
        return new FileEntry(new ResourceLocation(namespace, subPath + "/" + path));
    }

    @Override
    protected byte[] getFileContents(String path, FileEntry file) throws IOException {
        var resource = manager.getResource(file.identifier).orElse(null);
        if (resource == null) throw new FileOperationException(path, NO_SUCH_FILE);

        try (var stream = resource.open()) {
            return stream.readAllBytes();
        }
    }

    protected static final class FileEntry extends ArchiveMount.FileEntry<FileEntry> {
        final ResourceLocation identifier;

        FileEntry(ResourceLocation identifier) {
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
