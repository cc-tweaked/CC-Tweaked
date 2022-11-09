/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.integration;

import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.util.DirectVertexBuffer;

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
     * Determine if shaders may be used in the current session.
     *
     * @return Whether a shader mod is loaded.
     */
    public boolean isShaderMod() {
        return Optifine.isLoaded();
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

    private static class Storage {
        static final ShaderMod INSTANCE = ServiceLoader.load(Provider.class)
            .stream()
            .flatMap(x -> x.get().get().stream())
            .findFirst()
            .orElseGet(ShaderMod::new);
    }
}
