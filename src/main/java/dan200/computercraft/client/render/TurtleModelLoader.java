/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment( EnvType.CLIENT )
public final class TurtleModelLoader
{
    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();
    private static final ResourceLocation NORMAL_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_normal" );
    private static final ResourceLocation ADVANCED_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_advanced" );
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_colour" );

    private TurtleModelLoader()
    {
    }

    public boolean accepts( @Nonnull ResourceLocation name )
    {
        return name.getNamespace()
            .equals( ComputerCraft.MOD_ID ) && (name.getPath()
            .equals( "item/turtle_normal" ) || name.getPath()
            .equals( "item/turtle_advanced" ));
    }

    @Nonnull
    public UnbakedModel loadModel( @Nonnull ResourceLocation name )
    {
        if( name.getNamespace()
            .equals( ComputerCraft.MOD_ID ) )
        {
            switch( name.getPath() )
            {
                case "item/turtle_normal":
                    return new TurtleModel( NORMAL_TURTLE_MODEL );
                case "item/turtle_advanced":
                    return new TurtleModel( ADVANCED_TURTLE_MODEL );
            }
        }

        throw new IllegalStateException( "Loader does not accept " + name );
    }

    private static final class TurtleModel implements UnbakedModel
    {
        private final ResourceLocation family;

        private TurtleModel( ResourceLocation family )
        {
            this.family = family;
        }

        @Override
        public Collection<Material> getMaterials( Function<ResourceLocation, UnbakedModel> modelGetter,
                                                  Set<Pair<String, String>> missingTextureErrors )
        {
            return getDependencies()
                .stream()
                .flatMap( x -> modelGetter.apply( x )
                    .getMaterials( modelGetter, missingTextureErrors )
                    .stream() )
                .collect( Collectors.toSet() );
        }

        @Nonnull
        @Override
        public Collection<ResourceLocation> getDependencies()
        {
            return Arrays.asList( family, COLOUR_TURTLE_MODEL );
        }

        @Override
        public BakedModel bake( @Nonnull ModelBakery loader, @Nonnull Function<Material, TextureAtlasSprite> spriteGetter, @Nonnull ModelState state,
                                ResourceLocation modelId )
        {
            return new TurtleSmartItemModel( loader.getModel( family )
                .bake( loader, spriteGetter, state, modelId ),
                loader.getModel( COLOUR_TURTLE_MODEL )
                    .bake( loader, spriteGetter, state, modelId ) );
        }
    }
}
