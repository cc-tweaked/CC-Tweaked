/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.ModelRotationContainer;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TurtleItemUnbakedModel implements UnbakedModel
{
    private static final Identifier NORMAL_TURTLE_MODEL = new Identifier( "computercraft:block/turtle_normal" );
    private static final Identifier ADVANCED_TURTLE_MODEL = new Identifier( "computercraft:block/turtle_advanced" );
    private static final Identifier COLOUR_TURTLE_MODEL = new Identifier( "computercraft:block/turtle_colour" );
    private static final Identifier ELF_OVERLAY_MODEL = new Identifier( "computercraft:block/turtle_elf_overlay" );

    private final ComputerFamily family;

    public TurtleItemUnbakedModel( ComputerFamily family )
    {
        this.family = family;
    }

    @Override
    public Collection<Identifier> getModelDependencies()
    {
        return Arrays.asList(
            family == ComputerFamily.Advanced ? ADVANCED_TURTLE_MODEL : NORMAL_TURTLE_MODEL,
            COLOUR_TURTLE_MODEL, ELF_OVERLAY_MODEL
        );
    }

    @Override
    public Collection<Identifier> getTextureDependencies( Function<Identifier, UnbakedModel> loader, Set<String> failed )
    {
        return getModelDependencies().stream()
            .flatMap( x -> loader.apply( x ).getTextureDependencies( loader, failed ).stream() )
            .collect( Collectors.toSet() );
    }

    @Override
    @Nullable
    public BakedModel bake( ModelLoader loader, Function<Identifier, Sprite> texture, ModelRotationContainer transform )
    {
        return new TurtleSmartItemModel(
            loader.getOrLoadModel( family == ComputerFamily.Advanced ? ADVANCED_TURTLE_MODEL : NORMAL_TURTLE_MODEL )
                .bake( loader, texture, transform ),
            loader.getOrLoadModel( COLOUR_TURTLE_MODEL )
                .bake( loader, texture, transform )
        );
    }
}
