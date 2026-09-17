// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration;

import com.google.auto.service.AutoService;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.minecraft.util.LightCoordsUtil;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.IntFunction;

@AutoService(ShaderMod.Provider.class)
public class IrisShaderMod implements ShaderMod.Provider {
    @Override
    public Optional<ShaderMod> get() {
        return PlatformHelper.get().isModLoaded("iris") ? Optional.of(new Impl()) : Optional.empty();
    }

    private static final class Impl extends ShaderMod {
        @Override
        public boolean isRenderingShadowPass() {
            return IrisApi.getInstance().isRenderingShadowPass();
        }

        @Override
        public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter(VertexFormat format, int quadCount, IntFunction<ByteBuffer> makeBuffer) {
            // If we're using the vanilla vertex format, fall back to our default emitter.
            if (format == DirectFixedWidthFontRenderer.VERTEX_FORMAT) {
                return super.getQuadEmitter(format, quadCount, makeBuffer);
            }

            var sink = IrisApi.getInstance().createTextVertexSink(quadCount, makeBuffer);
            // TODO(26.3): Verify the vertex format matches what we expect once Iris has updated.
            return new IrisQuadEmitter(sink);
        }

        private static final class IrisQuadEmitter extends DirectFixedWidthFontRenderer.QuadEmitter {
            private final IrisTextVertexSink sink;

            private IrisQuadEmitter(IrisTextVertexSink sink) {
                this.sink = sink;
            }

            @Override
            public ByteBuffer byteBuffer() {
                return sink.getUnderlyingByteBuffer();
            }

            @Override
            public void quad(float x1, float y1, float x2, float y2, float z, int nativeColour, float u1, float v1, float u2, float v2) {
                sink.quad(x1, y1, x2, y2, z, nativeColour, u1, v1, u2, v2, LightCoordsUtil.FULL_BRIGHT);
            }
        }
    }
}
