// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps a computer's {@link BlockModel}/{@link BakedModel} to render the computer's cursor as an emissive quad.
 * <p>
 * While Fabric has a quite advanced rendering extension API (including support for custom materials), but unlike Forge
 * it doesn't expose this in the model JSON (though externals mods like <a href="https://github.com/vram-guild/json-model-extensions/">JMX</a>
 * do handle this).
 * <p>
 * Instead, we support emissive quads by injecting a custom {@linkplain ModelResourceProvider model loader/provider}
 * which targets a hard-coded list of computer models, and wraps the returned model in a custom
 * {@linkplain FabricBakedModel} implementation which renders specific quads as emissive.
 * <p>
 * See also the <code>assets/computercraft/models/block/computer_on.json</code> model, which is the base for all
 * emissive computer models.
 */
public final class EmissiveComputerModel {
    private static final Set<String> MODELS = Set.of(
        "item/computer_advanced",
        "block/computer_advanced_on",
        "block/computer_advanced_blinking",
        "item/computer_command",
        "block/computer_command_on",
        "block/computer_command_blinking",
        "item/computer_normal",
        "block/computer_normal_on",
        "block/computer_normal_blinking"
    );

    private EmissiveComputerModel() {
    }

    public static @Nullable UnbakedModel load(ResourceManager resources, ResourceLocation path) throws ModelProviderException {
        if (!path.getNamespace().equals(ComputerCraftAPI.MOD_ID) || !MODELS.contains(path.getPath())) return null;

        JsonObject json;
        try (var reader = resources.openAsReader(new ResourceLocation(path.getNamespace(), "models/" + path.getPath() + ".json"))) {
            json = GsonHelper.parse(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new ModelProviderException("Failed loading model " + path, e);
        }

        // Parse a subset of the model JSON
        var parent = new ResourceLocation(GsonHelper.getAsString(json, "parent"));

        Map<String, Either<Material, String>> textures = new HashMap<>();
        if (json.has("textures")) {
            var jsonObject = GsonHelper.getAsJsonObject(json, "textures");

            for (var entry : jsonObject.entrySet()) {
                var texture = entry.getValue().getAsString();
                textures.put(entry.getKey(), texture.startsWith("#")
                    ? Either.right(texture.substring(1))
                    : Either.left(new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(texture)))
                );
            }
        }

        return new Unbaked(parent, textures);
    }

    /**
     * An {@link UnbakedModel} which wraps the returned model using {@link Baked}.
     * <p>
     * This subclasses {@link BlockModel} to allow using these models as a parent of other models.
     */
    private static final class Unbaked extends BlockModel {
        Unbaked(ResourceLocation parent, Map<String, Either<Material, String>> materials) {
            super(parent, List.of(), materials, null, null, ItemTransforms.NO_TRANSFORMS, List.of());
        }

        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, ResourceLocation location) {
            var baked = super.bake(baker, spriteGetter, state, location);
            if (!hasTexture("cursor")) return baked;

            var render = RendererAccess.INSTANCE.getRenderer();
            if (render == null) return baked;

            return new Baked(
                baked,
                spriteGetter.apply(getMaterial("cursor")),
                render.materialFinder().find(),
                render.materialFinder().emissive(0, true).find()
            );
        }
    }

    /**
     * A {@link FabricBakedModel} which renders quads using the {@code "cursor"} texture as emissive.
     */
    private static final class Baked extends ForwardingBakedModel {
        private final TextureAtlasSprite cursor;
        private final RenderMaterial defaultMaterial;
        private final RenderMaterial emissiveMaterial;

        Baked(BakedModel wrapped, TextureAtlasSprite cursor, RenderMaterial defaultMaterial, RenderMaterial emissiveMaterial) {
            this.wrapped = wrapped;
            this.cursor = cursor;
            this.defaultMaterial = defaultMaterial;
            this.emissiveMaterial = emissiveMaterial;
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
                    emitter.fromVanilla(q, q.getSprite() == cursor ? emissiveMaterial : defaultMaterial, cullFace);
                    emitter.emit();
                }
            }
        }
    }
}
