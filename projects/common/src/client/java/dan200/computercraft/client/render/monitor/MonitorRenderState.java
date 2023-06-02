// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.monitor;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.render.vbo.DirectBuffers;
import dan200.computercraft.client.render.vbo.DirectVertexBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraft.core.BlockPos;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

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

    public int tboBuffer;
    public int tboTexture;
    public int tboUniform;
    public @Nullable DirectVertexBuffer backgroundBuffer;
    public @Nullable DirectVertexBuffer foregroundBuffer;

    /**
     * Create the appropriate buffer if needed.
     *
     * @param renderer The renderer to use.
     * @return If a buffer was created. This will return {@code false} if we already have an appropriate buffer,
     * or this mode does not require one.
     */
    public boolean createBuffer(MonitorRenderer renderer) {
        switch (renderer) {
            case TBO: {
                if (tboBuffer != 0) return false;

                deleteBuffers();

                tboBuffer = DirectBuffers.createBuffer();
                DirectBuffers.setEmptyBufferData(GL31.GL_TEXTURE_BUFFER, tboBuffer, GL15.GL_STATIC_DRAW);
                tboTexture = GlStateManager._genTexture();
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tboTexture);
                GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R8UI, tboBuffer);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);

                tboUniform = DirectBuffers.createBuffer();
                DirectBuffers.setEmptyBufferData(GL31.GL_UNIFORM_BUFFER, tboUniform, GL15.GL_STATIC_DRAW);

                addMonitor();
                return true;
            }

            case VBO:
                if (backgroundBuffer != null) return false;

                deleteBuffers();
                backgroundBuffer = new DirectVertexBuffer();
                foregroundBuffer = new DirectVertexBuffer();
                addMonitor();
                return true;

            default:
                return false;
        }
    }

    private void addMonitor() {
        synchronized (allMonitors) {
            allMonitors.add(this);
        }
    }

    private void deleteBuffers() {
        if (tboBuffer != 0) {
            DirectBuffers.deleteBuffer(GL31.GL_TEXTURE_BUFFER, tboBuffer);
            tboBuffer = 0;
        }

        if (tboTexture != 0) {
            GlStateManager._deleteTexture(tboTexture);
            tboTexture = 0;
        }

        if (tboUniform != 0) {
            DirectBuffers.deleteBuffer(GL31.GL_UNIFORM_BUFFER, tboUniform);
            tboUniform = 0;
        }

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
        if (tboBuffer != 0 || backgroundBuffer != null) {
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
