// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A standalone model.
 * <p>
 * This is very similar to vanilla's {@link BlockModelWrapper}, but suitable for use in both {@link ItemModel}s and
 * block models. This is primarily intended for use with {@link TurtleUpgradeModel}s.
 */
public final class StandaloneModel {
    private final List<BakedQuad> quads;
    private final boolean useBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final RenderType renderType;
    private final Supplier<Vector3f[]> extents;

    /**
     * Construct a new {@link StandaloneModel}.
     *
     * @param quads          The list of quads which form this model.
     * @param usesBlockLight Whether this uses block lighting. See {@link ItemStackRenderState.LayerRenderState#setUsesBlockLight(boolean)}.
     * @param particleIcon   The sprite for the model's particles. See {@link ItemStackRenderState.LayerRenderState#setParticleIcon(TextureAtlasSprite)}.
     * @param renderType     The render type for this model.
     */
    public StandaloneModel(List<BakedQuad> quads, boolean usesBlockLight, TextureAtlasSprite particleIcon, RenderType renderType) {
        this.quads = quads;
        this.useBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.renderType = renderType;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads));
    }

    /**
     * Load a model from a {@link ModelBaker} and bake it.
     *
     * @param model The model id to load.
     * @param baker The model baker.
     * @return The baked {@link StandaloneModel}.
     */
    public static StandaloneModel of(ResourceLocation model, ModelBaker baker) {
        return of(baker.getModel(model), baker);
    }

    /**
     * Bake a {@link ResolvedModel} into a {@link StandaloneModel}.
     *
     * @param model The resolved model.
     * @param baker The model baker.
     * @return The baked {@link StandaloneModel}.
     */
    public static StandaloneModel of(ResolvedModel model, ModelBaker baker) {
        return baker.compute(new CacheKey(model));
    }

    private record CacheKey(ResolvedModel model) implements ModelBaker.SharedOperationKey<StandaloneModel> {
        @Override
        public StandaloneModel compute(ModelBaker baker) {
            return ofUncached(model(), baker);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CacheKey(var otherModel) && model() == otherModel;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(model());
        }
    }

    private static StandaloneModel ofUncached(ResolvedModel model, ModelBaker baker) {
        var slots = model.getTopTextureSlots();
        return new StandaloneModel(
            model.bakeTopGeometry(slots, baker, BlockModelRotation.X0_Y0).getAll(),
            model.getTopGuiLight().lightLikeBlock(),
            model.resolveParticleSprite(slots, baker),
            Sheets.translucentItemSheet()
        );
    }

    /**
     * Set up an {@link ItemStackRenderState.LayerRenderState} to render this model.
     *
     * @param layer The layer to set up.
     * @see ItemModel#update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity, int)
     */
    public void setupItemLayer(ItemStackRenderState.LayerRenderState layer) {
        layer.setExtents(extents);
        layer.setRenderType(renderType);
        layer.setUsesBlockLight(useBlockLight);
        layer.setParticleIcon(particleIcon);
        layer.prepareQuadList().addAll(quads);
    }

    /**
     * Render the model directly.
     *
     * @param transform The current pose stack transformations.
     * @param buffers   The buffer source to use for rendering.
     * @param light     The current light texture coordinate.
     * @param overlay   The current overlay texture coordinate.
     */
    public void render(PoseStack transform, MultiBufferSource buffers, int light, int overlay) {
        render(transform, buffers, light, overlay, null);
    }

    /**
     * Render the model directly.
     *
     * @param transform The current pose stack transformations.
     * @param buffers   The buffer source to use for rendering.
     * @param light     The current light texture coordinate.
     * @param overlay   The current overlay texture coordinate.
     * @param tints     The tints for this model.
     */
    public void render(PoseStack transform, MultiBufferSource buffers, int light, int overlay, int @Nullable [] tints) {
        var pose = transform.last();
        var buffer = buffers.getBuffer(renderType);
        for (var quad : quads) {
            float r, g, b, a;
            var idx = quad.tintIndex();
            if (tints != null && idx >= 0 && idx < tints.length) {
                var tint = tints[idx];
                r = ARGB.red(tint) / 255.0f;
                g = ARGB.green(tint) / 255.0f;
                b = ARGB.blue(tint) / 255.0f;
                a = ARGB.alpha(tint) / 255.0f;
            } else {
                r = g = b = a = 1.0f;
            }

            buffer.putBulkData(pose, quad, r, g, b, a, light, overlay);
        }
    }
}
