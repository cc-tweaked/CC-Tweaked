// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import dan200.computercraft.core.util.Nullability;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A model wrapper which applies a {@link RenderMaterial#glint() glint}/foil to the original model.
 */
public final class FoiledModel extends ForwardingBakedModel implements RenderContext.QuadTransform {
    private final @Nullable Renderer renderer = RendererAccess.INSTANCE.getRenderer();
    private @Nullable RenderMaterial lastMaterial, lastFoiledMaterial;

    public FoiledModel(BakedModel model) {
        wrapped = model;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        context.pushTransform(this);
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        context.popTransform();
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        context.pushTransform(this);
        super.emitItemQuads(stack, randomSupplier, context);
        context.popTransform();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FoiledModel other && wrapped.equals(other.wrapped));
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode() ^ 1;
    }

    @Override
    public boolean transform(MutableQuadView quad) {
        if (renderer == null) return true;

        var material = quad.material();
        if (material == lastMaterial) {
            quad.material(Nullability.assertNonNull(lastFoiledMaterial));
        } else {
            lastMaterial = material;
            quad.material(lastFoiledMaterial = renderer.materialFinder().copyFrom(material).glint(TriState.TRUE).find());
        }

        return true;
    }
}
