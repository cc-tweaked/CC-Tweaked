// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.List;
import java.util.function.Function;

/**
 * A model loader for turtle item models.
 * <p>
 * This reads in the associated model file (typically {@code computercraft:block/turtle_xxx}) and wraps it in a
 * {@link TurtleModel}.
 */
public final class TurtleModelLoader implements IGeometryLoader<TurtleModelLoader.Unbaked> {
    private static final ResourceLocation COLOUR_TURTLE_MODEL = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_colour");

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader() {
    }

    @Override
    public Unbaked read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
        var model = ResourceLocation.parse(GsonHelper.getAsString(modelContents, "model"));
        return new Unbaked(model);
    }

    public static final class Unbaked implements IUnbakedGeometry<Unbaked> {
        private final ResourceLocation family;

        private Unbaked(ResourceLocation family) {
            this.family = family;
        }

        @Override
        public void resolveDependencies(UnbakedModel.Resolver modelGetter, IGeometryBakingContext context) {
            IUnbakedGeometry.super.resolveDependencies(modelGetter, context);
            modelGetter.resolve(family);
            modelGetter.resolve(COLOUR_TURTLE_MODEL);
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, List<ItemOverride> overrides) {
            var mainModel = bakery.bake(family, transform, spriteGetter);
            var colourModel = bakery.bake(COLOUR_TURTLE_MODEL, transform, spriteGetter);
            return new TurtleModel(mainModel, colourModel);
        }
    }
}
