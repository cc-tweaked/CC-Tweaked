// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A standalone model.
 * <p>
 * This is very similar to vanilla's {@link CuboidItemModelWrapper}, but suitable for use in both {@link ItemModel}s
 * and block models. This is primarily intended for use with {@link TurtleUpgradeModel}s.
 */
public final class StandaloneModel {
    private final List<BakedQuad> quads;
    private final boolean useBlockLight;
    private final Material.Baked particleIcon;
    private final RenderType renderType;
    private final Supplier<Vector3fc[]> extents;

    /**
     * Construct a new {@link StandaloneModel}.
     *
     * @param quads          The list of quads which form this model.
     * @param usesBlockLight Whether this uses block lighting. See {@link ItemStackRenderState.LayerRenderState#setUsesBlockLight(boolean)}.
     * @param particleIcon   The sprite for the model's particles. See {@link ItemStackRenderState.LayerRenderState#setParticleMaterial(Material.Baked)}.
     * @param renderType     The render type for this model.
     */
    public StandaloneModel(List<BakedQuad> quads, boolean usesBlockLight, Material.Baked particleIcon, RenderType renderType) {
        this.quads = quads;
        this.useBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.renderType = renderType;
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads));
    }

    /**
     * Load a model from a {@link ModelBaker} and bake it.
     *
     * @param model The model id to load.
     * @param baker The model baker.
     * @return The baked {@link StandaloneModel}.
     */
    public static StandaloneModel of(Identifier model, ModelBaker baker) {
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
        var quads = model.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY).getAll();
        return new StandaloneModel(
            quads,
            model.getTopGuiLight().lightLikeBlock(),
            model.resolveParticleMaterial(slots, baker),
            detectRenderType(quads)
        );
    }

    private static RenderType detectRenderType(List<BakedQuad> list) {
        if (list.isEmpty()) return Sheets.translucentItemSheet();

        var atlas = list.getFirst().materialInfo().sprite().atlasLocation();

        var mismatchedAtlas = list.stream()
            .map(x -> x.materialInfo().sprite().atlasLocation())
            .filter(x -> !x.equals(atlas)).findFirst().orElse(null);
        if (mismatchedAtlas != null) {
            throw new IllegalStateException("Multiple atlases used in model, expected " + atlas + ", but also got " + mismatchedAtlas);
        }

        var hasTranslucent = list.stream().anyMatch(x -> x.materialInfo().itemRenderType().hasBlending());
        if (atlas.equals(TextureAtlas.LOCATION_ITEMS)) {
            return hasTranslucent ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
        } else if (atlas.equals(TextureAtlas.LOCATION_BLOCKS)) {
            return hasTranslucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
        } else {
            throw new IllegalArgumentException("Atlas " + atlas + " can't be used for models");
        }
    }

    /**
     * Set up an {@link ItemStackRenderState.LayerRenderState} to render this model.
     *
     * @param layer The layer to set up.
     * @see ItemModel#update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, ItemOwner, int)
     */
    public void setupItemLayer(ItemStackRenderState.LayerRenderState layer) {
        layer.setExtents(extents);
        layer.setUsesBlockLight(useBlockLight);
        layer.setParticleMaterial(particleIcon);
        layer.prepareQuadList().addAll(quads);
    }

    /**
     * Render the model directly.
     *
     * @param transform The current pose stack transformations.
     * @param collector The node collector to render to.
     * @param light     The current light texture coordinate.
     * @param overlay   The current overlay texture coordinate.
     */
    public void submit(PoseStack transform, SubmitNodeCollector collector, int light, int overlay) {
        submit(transform, collector, light, overlay, -1, null);
    }

    /**
     * Render the model directly.
     *
     * @param transform        The current pose stack transformations.
     * @param collector        The node collector to render to.
     * @param light            The current light texture coordinate.
     * @param overlay          The current overlay texture coordinate.
     * @param tintColour       The tint for this model.
     * @param crumblingOverlay The current breaking progress.
     */
    public void submit(PoseStack transform, SubmitNodeCollector collector, int light, int overlay, int tintColour, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        collector.submitCustomGeometry(transform, renderType, (pose, buffer) -> render(pose, buffer, tintColour, light, overlay));

        if (crumblingOverlay != null && renderType.affectsCrumbling()) {
            // FIXME: We need a custom hook here, which renders to crumblingBufferSource. Currently the DESTROY_TYPES
            //  buffer gets flushed before the main model gets rendered.
            collector.submitCustomGeometry(transform, ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress()), (pose, buffer) ->
                render(pose, new SheetedDecalTextureGenerator(buffer, crumblingOverlay.cameraPose(), 1.0f), -1, light, overlay)
            );
        }
    }

    private void render(PoseStack.Pose pose, VertexConsumer buffer, int tintColour, int light, int overlay) {
        var instance = new QuadInstance();
        instance.setLightCoords(light);
        instance.setOverlayCoords(overlay);
        for (var quad : quads) {
            instance.setColor(quad.materialInfo().isTinted() ? tintColour : -1);
            buffer.putBakedQuad(pose, quad, instance);
        }
    }
}
