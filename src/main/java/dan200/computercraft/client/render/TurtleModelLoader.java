/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
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
    private static final Identifier COLOUR_TURTLE_MODEL = new Identifier( ComputerCraft.MOD_ID, "block/turtle_colour" );

    public static final TurtleModelLoader INSTANCE = new TurtleModelLoader();

    private TurtleModelLoader()
    {
    }

    @Override
    public void apply( @Nonnull ResourceManager manager )
    {
    }

    @Nonnull
    @Override
    public TurtleModel read( @Nonnull JsonDeserializationContext deserializationContext, @Nonnull JsonObject modelContents )
    {
        Identifier model = new Identifier( JsonHelper.getString( modelContents, "model" ) );
        return new TurtleModel( model );
    }

    public static final class TurtleModel implements IModelGeometry<TurtleModel>
    {
        private final Identifier family;

        private TurtleModel( Identifier family )
        {
            this.family = family;
        }

        @Override
        public Collection<SpriteIdentifier> getTextures( IModelConfiguration owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors )
        {
            Set<SpriteIdentifier> materials = new HashSet<>();
            materials.addAll( modelGetter.apply( family ).getTextureDependencies( modelGetter, missingTextureErrors ) );
            materials.addAll( modelGetter.apply( COLOUR_TURTLE_MODEL ).getTextureDependencies( modelGetter, missingTextureErrors ) );
            return materials;
        }

        @Override
        public BakedModel bake( IModelConfiguration owner, ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier modelLocation )
        {
            return new TurtleSmartItemModel(
                bakery.getBakedModel( family, transform, spriteGetter ),
                bakery.getBakedModel( COLOUR_TURTLE_MODEL, transform, spriteGetter )
            );
        }
    }
}
