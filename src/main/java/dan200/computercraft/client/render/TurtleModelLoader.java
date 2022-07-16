/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class TurtleModelLoader implements IGeometryLoader<TurtleModelLoader.TurtleModel>
{
    private static final ResourceLocation COLOUR_TURTLE_MODEL = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_colour" );

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader()
    {
    }

    @Nonnull
    @Override
    public TurtleModel read( @Nonnull JsonObject modelContents, @Nonnull JsonDeserializationContext deserializationContext )
    {
        ResourceLocation model = new ResourceLocation( GsonHelper.getAsString( modelContents, "model" ) );
        return new TurtleModel( model );
    }

    public static final class TurtleModel implements IUnbakedGeometry<TurtleModel>
    {
        private final ResourceLocation family;

        private TurtleModel( ResourceLocation family )
        {
            this.family = family;
        }

        @Override
        public Collection<Material> getMaterials( IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors )
        {
            Set<Material> materials = new HashSet<>();
            materials.addAll( modelGetter.apply( family ).getMaterials( modelGetter, missingTextureErrors ) );
            materials.addAll( modelGetter.apply( COLOUR_TURTLE_MODEL ).getMaterials( modelGetter, missingTextureErrors ) );
            return materials;
        }

        @Override
        public BakedModel bake( IGeometryBakingContext owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation modelLocation )
        {
            return new TurtleSmartItemModel(
                bakery.bake( family, transform, spriteGetter ),
                bakery.bake( COLOUR_TURTLE_MODEL, transform, spriteGetter )
            );
        }
    }
}
