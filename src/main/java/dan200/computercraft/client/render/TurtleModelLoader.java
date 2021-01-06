/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class TurtleModelLoader implements IModelLoader<TurtleModelLoader.TurtleModel>
{
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_colour" );

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader()
    {
    }

    @Override
    public void onResourceManagerReload( @Nonnull IResourceManager manager )
    {
    }

    @Nonnull
    @Override
    public TurtleModel read( @Nonnull JsonDeserializationContext deserializationContext, @Nonnull JsonObject modelContents )
    {
        ResourceLocation model = new ResourceLocation( JSONUtils.getString( modelContents, "model" ) );
        return new TurtleModel( model );
    }

    public static final class TurtleModel implements IModelGeometry<TurtleModel>
    {
        private final ResourceLocation family;

        private TurtleModel( ResourceLocation family )
        {
            this.family = family;
        }

        @Override
        public Collection<RenderMaterial> getTextures( IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors )
        {
            Set<RenderMaterial> materials = new HashSet<>();
            materials.addAll( modelGetter.apply( family ).getTextures( modelGetter, missingTextureErrors ) );
            materials.addAll( modelGetter.apply( COLOUR_TURTLE_MODEL ).getTextures( modelGetter, missingTextureErrors ) );
            return materials;
        }

        @Override
        public IBakedModel bake( IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform transform, ItemOverrideList overrides, ResourceLocation modelLocation )
        {
            return new TurtleSmartItemModel(
                bakery.getBakedModel( family, transform, spriteGetter ),
                bakery.getBakedModel( COLOUR_TURTLE_MODEL, transform, spriteGetter )
            );
        }
    }
}
