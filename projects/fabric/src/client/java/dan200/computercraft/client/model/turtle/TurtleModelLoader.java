// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A model "loader" (the concept doesn't quite exist in the same way as it does on Forge) for turtle item models.
 * <p>
 * This reads in the associated model file (typically {@code computercraft:block/turtle_xxx}) and wraps it in a
 * {@link TurtleModel}.
 */
public final class TurtleModelLoader {
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_colour");

    private TurtleModelLoader() {
    }

    public static @Nullable UnbakedModel load(ResourceManager resources, ResourceLocation path) throws ModelProviderException {
        if (!path.getNamespace().equals(ComputerCraftAPI.MOD_ID)) return null;
        if (!path.getPath().equals("item/turtle_normal") && !path.getPath().equals("item/turtle_advanced")) {
            return null;
        }

        try (var reader = resources.openAsReader(new ResourceLocation(path.getNamespace(), "models/" + path.getPath() + ".json"))) {
            var modelContents = GsonHelper.parse(reader).getAsJsonObject();

            var loader = GsonHelper.getAsString(modelContents, "loader", null);
            if (!Objects.equals(loader, ComputerCraftAPI.MOD_ID + ":turtle")) return null;

            var model = new ResourceLocation(GsonHelper.getAsString(modelContents, "model"));
            return new Unbaked(model);
        } catch (IOException e) {
            throw new ModelProviderException("Failed loading model " + path, e);
        }
    }

    public static final class Unbaked implements UnbakedModel {
        private final ResourceLocation model;

        private Unbaked(ResourceLocation model) {
            this.model = model;
        }

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return List.of(model, COLOUR_TURTLE_MODEL);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {
            function.apply(model).resolveParents(function);
            function.apply(COLOUR_TURTLE_MODEL).resolveParents(function);
        }

        @Override
        public BakedModel bake(ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ResourceLocation location) {
            var mainModel = bakery.bake(model, transform);
            if (mainModel == null) throw new NullPointerException(model + " failed to bake");

            var colourModel = bakery.bake(COLOUR_TURTLE_MODEL, transform);
            if (colourModel == null) throw new NullPointerException(COLOUR_TURTLE_MODEL + " failed to bake");

            return new TurtleModel(mainModel, colourModel);
        }
    }
}
