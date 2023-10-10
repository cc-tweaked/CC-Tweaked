// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration;

import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.vbo.DirectVertexBuffer;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.IntFunction;

/**
 * Find the currently loaded shader mod (if present) and provides utilities for interacting with it.
 */
public class ShaderMod {
    public static ShaderMod get() {
        return Storage.INSTANCE;
    }

    /**
     * Check whether we're currently rendering shadows. Rendering may fall back to a faster but less detailed pass.
     *
     * @return Whether we're rendering shadows.
     */
    public boolean isRenderingShadowPass() {
        return false;
    }

    /**
     * Get an appropriate quad emitter for use with {@link DirectVertexBuffer} and {@link DirectFixedWidthFontRenderer} .
     *
     * @param vertexCount The number of vertices.
     * @param makeBuffer  A function to allocate a temporary buffer.
     * @return The quad emitter.
     */
    public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter(int vertexCount, IntFunction<ByteBuffer> makeBuffer) {
        return new DirectFixedWidthFontRenderer.ByteBufferEmitter(
            makeBuffer.apply(RenderTypes.TERMINAL.format().getVertexSize() * vertexCount * 4)
        );
    }

    public interface Provider {
        Optional<ShaderMod> get();
    }

    private static final class Storage {
        static final ShaderMod INSTANCE = ServiceLoader.load(Provider.class)
            .stream()
            .flatMap(x -> x.get().get().stream())
            .findFirst()
            .orElseGet(ShaderMod::new);
    }
}
