/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.model.turtle.TurtleModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

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
        public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
            Set<Material> materials = new HashSet<>();
            materials.addAll(modelGetter.apply(family).getMaterials(modelGetter, missingTextureErrors));
            materials.addAll(modelGetter.apply(COLOUR_TURTLE_MODEL).getMaterials(modelGetter, missingTextureErrors));
            return materials;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation modelLocation) {
            var mainModel = bakery.bake(family, transform, spriteGetter);
            if (mainModel == null) throw new NullPointerException(family + " failed to bake");

            var colourModel = bakery.bake(COLOUR_TURTLE_MODEL, transform, spriteGetter);
            if (colourModel == null) throw new NullPointerException(COLOUR_TURTLE_MODEL + " failed to bake");

            return new TurtleModel(mainModel, colourModel);
        }
    }
}
