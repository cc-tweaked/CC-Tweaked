/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.common.ClientTerminal;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment (EnvType.CLIENT)
public final class ClientMonitor extends ClientTerminal {
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;

    public int tboBuffer;
    public int tboTexture;
    public VertexBuffer buffer;

    public ClientMonitor(boolean colour, TileMonitor origin) {
        super(colour);
        this.origin = origin;
    }

    @Environment (EnvType.CLIENT)
    public static void destroyAll() {
        synchronized (allMonitors) {
            for (Iterator<ClientMonitor> iterator = allMonitors.iterator(); iterator.hasNext(); ) {
                ClientMonitor monitor = iterator.next();
                monitor.deleteBuffers();

                iterator.remove();
            }
        }
    }

    public TileMonitor getOrigin() {
        return this.origin;
    }

    /**
     * Create the appropriate buffer if needed.
     *
     * @param renderer The renderer to use. This can be fetched from {@link MonitorRenderer#current()}.
     * @return If a buffer was created. This will return {@code false} if we already have an appropriate buffer, or this mode does not require one.
     */
    @Environment (EnvType.CLIENT)
    public boolean createBuffer(MonitorRenderer renderer) {
        switch (renderer) {
        case TBO: {
            if (this.tboBuffer != 0) {
                return false;
            }

            this.deleteBuffers();

            this.tboBuffer = GlStateManager.genBuffers();
            GlStateManager.bindBuffers(GL31.GL_TEXTURE_BUFFER, this.tboBuffer);
            GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, 0, GL15.GL_STATIC_DRAW);
            this.tboTexture = GlStateManager.genTextures();
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.tboTexture);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R8, this.tboBuffer);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);

            GlStateManager.bindBuffers(GL31.GL_TEXTURE_BUFFER, 0);

            this.addMonitor();
            return true;
        }

        case VBO:
            if (this.buffer != null) {
                return false;
            }

            this.deleteBuffers();
            this.buffer = new VertexBuffer(FixedWidthFontRenderer.TYPE.getVertexFormat());
            this.addMonitor();
            return true;

        default:
            return false;
        }
    }

    private void deleteBuffers() {

        if (this.tboBuffer != 0) {
            RenderSystem.glDeleteBuffers(this.tboBuffer);
            this.tboBuffer = 0;
        }

        if (this.tboTexture != 0) {
            GlStateManager.deleteTexture(this.tboTexture);
            this.tboTexture = 0;
        }

        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
    }

    private void addMonitor() {
        synchronized (allMonitors) {
            allMonitors.add(this);
        }
    }

    @Environment (EnvType.CLIENT)
    public void destroy() {
        if (this.tboBuffer != 0 || this.buffer != null) {
            synchronized (allMonitors) {
                allMonitors.remove(this);
            }

            this.deleteBuffers();
        }
    }
}
