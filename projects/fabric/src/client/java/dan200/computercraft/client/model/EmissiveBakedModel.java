// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Wraps an arbitrary {@link BakedModel} to render a single texture as emissive.
 * <p>
 * While Fabric has a quite advanced rendering extension API (including support for custom materials), but unlike Forge
 * it doesn't expose this in the model JSON (though externals mods like <a href="https://github.com/vram-guild/json-model-extensions/">JMX</a>
 * do handle this).
 * <p>
 * Instead, we support emissive quads by injecting a {@linkplain CustomModelLoader custom model loader} which wraps the
 * baked model in a {@link EmissiveBakedModel}, which renders specific quads as emissive.
 */
public final class EmissiveBakedModel extends ForwardingBakedModel {
    private final TextureAtlasSprite emissiveTexture;
    private final RenderMaterial defaultMaterial;
    private final RenderMaterial emissiveMaterial;

    private EmissiveBakedModel(BakedModel wrapped, TextureAtlasSprite emissiveTexture, RenderMaterial defaultMaterial, RenderMaterial emissiveMaterial) {
        this.wrapped = wrapped;
        this.emissiveTexture = emissiveTexture;
        this.defaultMaterial = defaultMaterial;
        this.emissiveMaterial = emissiveMaterial;
    }

    public static BakedModel wrap(BakedModel model, TextureAtlasSprite emissiveTexture) {
        var renderer = RendererAccess.INSTANCE.getRenderer();
        return renderer == null ? model : new EmissiveBakedModel(
            model,
            emissiveTexture,
            renderer.materialFinder().find(),
            renderer.materialFinder().emissive(true).find()
        );
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        emitQuads(context, state, randomSupplier.get());
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        emitQuads(context, null, randomSupplier.get());
    }

    private void emitQuads(RenderContext context, @Nullable BlockState state, RandomSource random) {
        var emitter = context.getEmitter();
        for (var faceIdx = 0; faceIdx <= ModelHelper.NULL_FACE_ID; faceIdx++) {
            var cullFace = ModelHelper.faceFromIndex(faceIdx);
            var quads = wrapped.getQuads(state, cullFace, random);

            var count = quads.size();
            for (var i = 0; i < count; i++) {
                final var q = quads.get(i);
                emitter.fromVanilla(q, q.getSprite() == emissiveTexture ? emissiveMaterial : defaultMaterial, cullFace);
                emitter.emit();
            }
        }
    }
}
