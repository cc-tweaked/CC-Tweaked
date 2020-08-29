/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

public final class TurtleModelLoader {
    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();
    private static final Identifier NORMAL_TURTLE_MODEL = new Identifier(ComputerCraft.MOD_ID, "block/turtle_normal");
    private static final Identifier ADVANCED_TURTLE_MODEL = new Identifier(ComputerCraft.MOD_ID, "block/turtle_advanced");
    private static final Identifier COLOUR_TURTLE_MODEL = new Identifier(ComputerCraft.MOD_ID, "block/turtle_colour");

    private TurtleModelLoader() {
    }

    public boolean accepts(@Nonnull Identifier name) {
        return name.getNamespace()
                   .equals(ComputerCraft.MOD_ID) && (name.getPath()
                                                         .equals("item/turtle_normal") || name.getPath()
                                                                                              .equals("item/turtle_advanced"));
    }

    @Nonnull
    public UnbakedModel loadModel(@Nonnull Identifier name) {
        if (name.getNamespace()
                .equals(ComputerCraft.MOD_ID)) {
            switch (name.getPath()) {
            case "item/turtle_normal":
                return new TurtleModel(NORMAL_TURTLE_MODEL);
            case "item/turtle_advanced":
                return new TurtleModel(ADVANCED_TURTLE_MODEL);
            }
        }

        throw new IllegalStateException("Loader does not accept " + name);
    }

    private static final class TurtleModel implements UnbakedModel {
        private final Identifier family;

        private TurtleModel(Identifier family) {this.family = family;}

        @Nonnull
        @Override
        public Collection<Identifier> getTextureDependencies(@Nonnull Function<Identifier, UnbakedModel> modelGetter,
                                                             @Nonnull Set<String> missingTextureErrors) {
            return this.getModelDependencies().stream()
                       .flatMap(x -> modelGetter.apply(x)
                                                                  .getTextureDependencies(modelGetter, missingTextureErrors)
                                                                  .stream())
                       .collect(Collectors.toSet());
        }

        @Nonnull
        @Override
        public Collection<Identifier> getModelDependencies() {
            return Arrays.asList(this.family, COLOUR_TURTLE_MODEL);
        }

        @Nullable
        @Override
        public BakedModel bake(@Nonnull ModelLoader loader, @Nonnull Function<Identifier, Sprite> spriteGetter, @Nonnull ModelBakeSettings state) {
            return new TurtleSmartItemModel(loader,
                                            loader.getOrLoadModel(this.family)
                                                  .bake(loader, spriteGetter, state),
                                            loader.getOrLoadModel(COLOUR_TURTLE_MODEL)
                                                  .bake(loader, spriteGetter, state));
        }
    }
}
