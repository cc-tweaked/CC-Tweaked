/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TurtleModelLoader implements ICustomModelLoader
{
    private static final ResourceLocation NORMAL_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_normal" );
    private static final ResourceLocation ADVANCED_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_advanced" );
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_colour" );

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader()
    {
    }

    @Override
    public void onResourceManagerReload( @Nonnull IResourceManager manager )
    {
    }

    @Override
    public boolean accepts( @Nonnull ResourceLocation name )
    {
        return name.getNamespace().equals( ComputerCraft.MOD_ID )
            && (name.getPath().equals( "item/turtle_normal" ) || name.getPath().equals( "item/turtle_advanced" ));
    }

    @Nonnull
    @Override
    public IUnbakedModel loadModel( @Nonnull ResourceLocation name )
    {
        if( name.getNamespace().equals( ComputerCraft.MOD_ID ) )
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

    private static final class TurtleModel implements IUnbakedModel
    {
        private final ResourceLocation family;

        private TurtleModel( ResourceLocation family )
        {
            this.family = family;
        }

        @Nonnull
        @Override
        public Collection<ResourceLocation> getDependencies()
        {
            return Arrays.asList( family, COLOUR_TURTLE_MODEL );
        }

        @Nonnull
        @Override
        public Collection<ResourceLocation> getTextures( @Nonnull Function<ResourceLocation, IUnbakedModel> modelGetter, @Nonnull Set<String> missingTextureErrors )
        {
            return getDependencies().stream()
                .flatMap( x -> modelGetter.apply( x ).getTextures( modelGetter, missingTextureErrors ).stream() )
                .collect( Collectors.toSet() );
        }

        @Nullable
        @Override
        public IBakedModel bake( @Nonnull Function<ResourceLocation, IUnbakedModel> modelGetter, @Nonnull Function<ResourceLocation, TextureAtlasSprite> spriteGetter, @Nonnull IModelState state, boolean uvlock, @Nonnull VertexFormat format )
        {
            return new TurtleSmartItemModel(
                modelGetter.apply( family ).bake( modelGetter, spriteGetter, state, uvlock, format ),
                modelGetter.apply( COLOUR_TURTLE_MODEL ).bake( modelGetter, spriteGetter, state, uvlock, format )
            );
        }
    }
}
