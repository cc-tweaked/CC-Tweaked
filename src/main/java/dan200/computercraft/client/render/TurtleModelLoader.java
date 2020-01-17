/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nonnull;
import java.util.function.Function;

public final class TurtleModelLoader implements ICustomModelLoader
{
    private static final ResourceLocation NORMAL_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle" );
    private static final ResourceLocation ADVANCED_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/advanced_turtle" );
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_white" );

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
            && (name.getPath().equals( "turtle" ) || name.getPath().equals( "turtle_advanced" ));
    }

    @Nonnull
    @Override
    public IModel loadModel( @Nonnull ResourceLocation name ) throws Exception
    {
        if( name.getNamespace().equals( ComputerCraft.MOD_ID ) )
        {
            IModel colourModel = ModelLoaderRegistry.getModel( COLOUR_TURTLE_MODEL );
            switch( name.getPath() )
            {
                case "turtle":
                    return new TurtleModel( ModelLoaderRegistry.getModel( NORMAL_TURTLE_MODEL ), colourModel );
                case "turtle_advanced":
                    return new TurtleModel( ModelLoaderRegistry.getModel( ADVANCED_TURTLE_MODEL ), colourModel );
            }
        }

        throw new IllegalStateException( "Loader does not accept " + name );
    }

    private static final class TurtleModel implements IModel
    {
        private final IModel family;
        private final IModel colour;

        private TurtleModel( IModel family, IModel colour )
        {
            this.family = family;
            this.colour = colour;
        }

        @Nonnull
        @Override
        public IBakedModel bake( @Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> function )
        {
            return new TurtleSmartItemModel(
                family.bake( state, format, function ),
                colour.bake( state, format, function )
            );
        }

        private TurtleModel copy( IModel family, IModel colour )
        {
            return this.family == family && this.colour == colour ? this : new TurtleModel( family, colour );
        }

        @Nonnull
        @Override
        public IModel smoothLighting( boolean value )
        {
            return copy( family.smoothLighting( value ), colour.smoothLighting( value ) );
        }

        @Nonnull
        @Override
        public IModel gui3d( boolean value )
        {
            return copy( family.gui3d( value ), colour.gui3d( value ) );
        }

        @Nonnull
        @Override
        public IModel uvlock( boolean value )
        {
            return copy( family.uvlock( value ), colour.uvlock( value ) );
        }

        @Nonnull
        @Override
        public IModel retexture( ImmutableMap<String, String> textures )
        {
            return copy( family.retexture( textures ), colour.retexture( textures ) );
        }
    }
}
