// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.monitor;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.mojang.blaze3d.buffers.GpuBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds the client-side state of a monitor. This both tracks the last place a monitor was rendered at (see the comments
 * in {@link MonitorBlockEntityRenderer}) and the current OpenGL buffers allocated for this object.
 * <p>
 * This is automatically cleared by {@link dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity} when the
 * entity is unloaded on the client side (see {@link MonitorRenderState#close()}).
 */
public final class MonitorRenderState implements ClientMonitor.RenderState {
    @GuardedBy("allMonitors")
    private static final Set<MonitorRenderState> allMonitors = new HashSet<>();

    long lastRenderFrame = -1;
    @Nullable
    BlockPos lastRenderPos = null;

    @Nullable
    GpuBuffer vertexBuffer;

    int indexAfterBackground;
    int indexAfterForeground;
    int indexAfterCursor;

    void register() {
        if (vertexBuffer != null) return;

        synchronized (allMonitors) {
            allMonitors.add(this);
        }
    }

    private void deleteBuffers() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    @Override
    public void close() {
        if (vertexBuffer != null) {
            synchronized (allMonitors) {
                allMonitors.remove(this);
            }

            deleteBuffers();
        }
    }

    public static void destroyAll() {
        synchronized (allMonitors) {
            for (var iterator = allMonitors.iterator(); iterator.hasNext(); ) {
                var monitor = iterator.next();
                monitor.deleteBuffers();
                iterator.remove();
            }
        }
    }
}
