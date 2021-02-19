/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A model to render, combined with a transformation matrix to apply.
 */
public final class TransformedModel
{
    private final IBakedModel model;
    private final TransformationMatrix matrix;

    public TransformedModel( @Nonnull IBakedModel model, @Nonnull TransformationMatrix matrix )
    {
        this.model = Objects.requireNonNull( model );
        this.matrix = Objects.requireNonNull( matrix );
    }

    public TransformedModel( @Nonnull IBakedModel model )
    {
        this.model = Objects.requireNonNull( model );
        this.matrix = TransformationMatrix.identity();
    }

    public static TransformedModel of( @Nonnull ModelResourceLocation location )
    {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel( modelManager.getModel( location ) );
    }

    public static TransformedModel of( @Nonnull ItemStack item, @Nonnull TransformationMatrix transform )
    {
        IBakedModel model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel( item );
        return new TransformedModel( model, transform );
    }

    @Nonnull
    public IBakedModel getModel()
    {
        return model;
    }

    @Nonnull
    public TransformationMatrix getMatrix()
    {
        return matrix;
    }
}
