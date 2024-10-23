// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.monitor;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the client-side state of a monitor. This both tracks the last place a monitor was rendered at (see the comments
 * in {@link MonitorBlockEntityRenderer}) and the current OpenGL buffers allocated for this object.
 * <p>
 * This is automatically cleared by {@link dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity} when the
 * entity is unloaded on the client side (see {@link MonitorRenderState#close()}).
 */
public class MonitorRenderState implements ClientMonitor.RenderState {
    @GuardedBy("allMonitors")
    private static final Set<MonitorRenderState> allMonitors = new HashSet<>();

    public long lastRenderFrame = -1;
    public @Nullable BlockPos lastRenderPos = null;

    public @Nullable VertexBuffer backgroundBuffer;
    public @Nullable VertexBuffer foregroundBuffer;

    /**
     * Create the appropriate buffer if needed.
     *
     * @return If a buffer was created. This will return {@code false} if we already have an appropriate buffer,
     * or this mode does not require one.
     */
    public boolean createBuffer() {
        if (backgroundBuffer != null) return false;

        deleteBuffers();
        backgroundBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
        foregroundBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
        addMonitor();
        return true;
    }

    private void addMonitor() {
        synchronized (allMonitors) {
            allMonitors.add(this);
        }
    }

    private void deleteBuffers() {
        if (backgroundBuffer != null) {
            backgroundBuffer.close();
            backgroundBuffer = null;
        }

        if (foregroundBuffer != null) {
            foregroundBuffer.close();
            foregroundBuffer = null;
        }
    }

    @Override
    public void close() {
        if (backgroundBuffer != null) {
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
