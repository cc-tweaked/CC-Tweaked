// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * A model loader for turtle item models.
 * <p>
 * This reads in the associated model file (typically {@code computercraft:block/turtle_xxx}) and wraps it in a
 * {@link TurtleModel}.
 */
public final class TurtleModelLoader implements IGeometryLoader<TurtleModelLoader.Unbaked> {
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_colour");

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader() {
    }

    @Override
    public Unbaked read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
        var model = new ResourceLocation(GsonHelper.getAsString(modelContents, "model"));
        return new Unbaked(model);
    }

    public static final class Unbaked implements IUnbakedGeometry<Unbaked> {
        private final ResourceLocation family;

        private Unbaked(ResourceLocation family) {
            this.family = family;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext owner, ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation modelLocation) {
            var mainModel = bakery.bake(family, transform, spriteGetter);
            if (mainModel == null) throw new NullPointerException(family + " failed to bake");

            var colourModel = bakery.bake(COLOUR_TURTLE_MODEL, transform, spriteGetter);
            if (colourModel == null) throw new NullPointerException(COLOUR_TURTLE_MODEL + " failed to bake");

            return new TurtleModel(mainModel, colourModel);
        }
    }
}
