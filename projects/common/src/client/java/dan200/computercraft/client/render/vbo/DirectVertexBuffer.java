// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.vbo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

/**
 * A version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly.
 * <p>
 * This should probably be its own class (rather than subclassing), but I need access to {@link VertexBuffer#drawWithShader}.
 */
public class DirectVertexBuffer extends VertexBuffer {
    private int actualIndexCount;

    public DirectVertexBuffer() {
        super(Usage.STATIC);
        if (DirectBuffers.HAS_DSA) {
            RenderSystem.glDeleteBuffers(vertexBufferId);
            if (DirectBuffers.ON_LINUX) BufferUploader.reset(); // See comment on DirectBuffers.deleteBuffer.
            vertexBufferId = GL45C.glCreateBuffers();
        }
    }

    public void upload(int vertexCount, VertexFormat.Mode mode, VertexFormat format, ByteBuffer buffer) {
        bind();

        this.mode = mode;
        actualIndexCount = indexCount = mode.indexCount(vertexCount);
        indexType = VertexFormat.IndexType.SHORT;

        RenderSystem.assertOnRenderThread();

        DirectBuffers.setBufferData(GL15.GL_ARRAY_BUFFER, vertexBufferId, buffer, GL15.GL_STATIC_DRAW);
        if (format != this.format) {
            if (this.format != null) this.format.clearBufferState();
            this.format = format;

            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vertexBufferId);
            format.setupBufferState();
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        }

        var indexBuffer = RenderSystem.getSequentialBuffer(mode);
        if (indexBuffer != sequentialIndices || !indexBuffer.hasStorage(indexCount)) {
            indexBuffer.bind(indexCount);
            sequentialIndices = indexBuffer;
        }
    }

    public void drawWithShader(Matrix4f modelView, Matrix4f projection, ShaderInstance shader, int indexCount) {
        this.indexCount = indexCount;
        drawWithShader(modelView, projection, shader);
        this.indexCount = actualIndexCount;
    }

    public int getIndexCount() {
        return actualIndexCount;
    }

    @Override
    public void close() {
        super.close();
        if (DirectBuffers.ON_LINUX) BufferUploader.reset(); // See comment on DirectBuffers.deleteBuffer.
    }
}
