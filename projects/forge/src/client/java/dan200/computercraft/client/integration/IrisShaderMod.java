// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.shared.util.ARGB32;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.minecraftforge.fml.ModList;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * A {@link ShaderMod} for Oculus (the Forge Iris port).
 */
@AutoService(ShaderMod.Provider.class)
public class IrisShaderMod implements ShaderMod.Provider {
    @Override
    public Optional<ShaderMod> get() {
        return ModList.get().isLoaded("oculus") ? Optional.of(new Impl()) : Optional.empty();
    }

    private static final class Impl extends ShaderMod {
        @Override
        public boolean isRenderingShadowPass() {
            return IrisApi.getInstance().isRenderingShadowPass();
        }

        @Override
        public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter(int vertexCount, IntFunction<ByteBuffer> makeBuffer) {
            return IrisApi.getInstance().getMinorApiRevision() >= 1
                ? new IrisQuadEmitter(vertexCount, makeBuffer)
                : super.getQuadEmitter(vertexCount, makeBuffer);
        }

        private static final class IrisQuadEmitter implements DirectFixedWidthFontRenderer.QuadEmitter {
            private final IrisTextVertexSink sink;

            private IrisQuadEmitter(int vertexCount, IntFunction<ByteBuffer> makeBuffer) {
                sink = IrisApi.getInstance().createTextVertexSink(vertexCount, makeBuffer);
            }

            @Override
            public VertexFormat format() {
                return sink.getUnderlyingVertexFormat();
            }

            @Override
            public ByteBuffer buffer() {
                return sink.getUnderlyingByteBuffer();
            }

            @Override
            public void quad(float x1, float y1, float x2, float y2, float z, int colour, float u1, float v1, float u2, float v2) {
                sink.quad(x1, y1, x2, y2, z, ARGB32.toABGR32(colour), u1, v1, u2, v2, RenderTypes.FULL_BRIGHT_LIGHTMAP);
            }
        }
    }
}
