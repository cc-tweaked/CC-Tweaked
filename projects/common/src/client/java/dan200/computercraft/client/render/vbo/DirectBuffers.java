// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.vbo;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.Util;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

/**
 * Provides utilities to interact with OpenGL's buffer objects, either using direct state access or binding/unbinding
 * it.
 */
public class DirectBuffers {
    public static final boolean HAS_DSA;
    static final boolean ON_LINUX = Util.getPlatform() == Util.OS.LINUX;

    static {
        var capabilities = GL.getCapabilities();
        HAS_DSA = capabilities.OpenGL45 || capabilities.GL_ARB_direct_state_access;
    }

    public static int createBuffer() {
        return HAS_DSA ? GL45C.glCreateBuffers() : GL15C.glGenBuffers();
    }

    /**
     * Delete a previously created buffer.
     * <p>
     * On Linux, {@link GlStateManager#_glDeleteBuffers(int)} clears a buffer before deleting it. However, this involves
     * binding and unbinding the buffer, conflicting with {@link BufferUploader}'s cache. This deletion method uses
     * our existing {@link #setEmptyBufferData(int, int, int)}, which correctly handles clearing the buffer.
     *
     * @param type The buffer's type.
     * @param id   The buffer's ID.
     */
    public static void deleteBuffer(int type, int id) {
        RenderSystem.assertOnRenderThread();
        if (ON_LINUX) DirectBuffers.setEmptyBufferData(type, id, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glDeleteBuffers(id);
    }

    public static void setBufferData(int type, int id, ByteBuffer buffer, int flags) {
        if (HAS_DSA) {
            GL45C.glNamedBufferData(id, buffer, flags);
        } else {
            if (type == GL15C.GL_ARRAY_BUFFER) BufferUploader.reset();
            GlStateManager._glBindBuffer(type, id);
            GlStateManager._glBufferData(type, buffer, flags);
            GlStateManager._glBindBuffer(type, 0);
        }
    }

    public static void setEmptyBufferData(int type, int id, int flags) {
        if (HAS_DSA) {
            GL45C.glNamedBufferData(id, 0, flags);
        } else {
            if (type == GL15C.GL_ARRAY_BUFFER) BufferUploader.reset();
            GlStateManager._glBindBuffer(type, id);
            GlStateManager._glBufferData(type, 0, flags);
            GlStateManager._glBindBuffer(type, 0);
        }
    }
}
