// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model.turtle;

import com.google.gson.JsonObject;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.function.Function;

/**
 * A {@link UnbakedModel} for {@link TurtleModel}s.
 * <p>
 * This reads in the associated model file (typically {@code computercraft:block/turtle_xxx}) and wraps it in a
 * {@link TurtleModel}.
 */
public final class UnbakedTurtleModel implements UnbakedModel {
    private static final ResourceLocation COLOUR_TURTLE_MODEL = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_colour");

    private final ResourceLocation model;

    private UnbakedTurtleModel(ResourceLocation model) {
        this.model = model;
    }

    public static UnbakedModel parse(JsonObject json) {
        var model = ResourceLocation.parse(GsonHelper.getAsString(json, "model"));
        return new UnbakedTurtleModel(model);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        resolver.resolve(model);
        resolver.resolve(COLOUR_TURTLE_MODEL);
    }

    @Override
    public BakedModel bake(ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform) {
        var mainModel = bakery.bake(model, transform);
        var colourModel = bakery.bake(COLOUR_TURTLE_MODEL, transform);
        return new TurtleModel(bakery, mainModel, colourModel);
    }
}
