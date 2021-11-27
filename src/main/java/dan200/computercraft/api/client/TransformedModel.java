/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A model to render, combined with a transformation matrix to apply.
 */
@Environment( EnvType.CLIENT )
public final class TransformedModel
{
    private final BakedModel model;
    private final Transformation matrix;

    public TransformedModel( @Nonnull BakedModel model, @Nonnull Transformation matrix )
    {
        this.model = Objects.requireNonNull( model );
        this.matrix = Objects.requireNonNull( matrix );
    }

    public TransformedModel( @Nonnull BakedModel model )
    {
        this.model = Objects.requireNonNull( model );
        matrix = Transformation.identity();
    }

    public static TransformedModel of( @Nonnull ModelResourceLocation location )
    {
        ModelManager modelManager = Minecraft.getInstance()
            .getModelManager();
        return new TransformedModel( modelManager.getModel( location ) );
    }

    public static TransformedModel of( @Nonnull ItemStack item, @Nonnull Transformation transform )
    {
        BakedModel model = Minecraft.getInstance()
            .getItemRenderer()
            .getItemModelShaper()
            .getItemModel( item );
        return new TransformedModel( model, transform );
    }

    @Nonnull
    public BakedModel getModel()
    {
        return model;
    }

    @Nonnull
    public Transformation getMatrix()
    {
        return matrix;
    }

    public void push( PoseStack matrixStack )
    {
        matrixStack.pushPose();

        Vector3f translation = matrix.getTranslation();
        matrixStack.translate( translation.x(), translation.y(), translation.z() );

        matrixStack.mulPose( matrix.getLeftRotation() );

        Vector3f scale = matrix.getScale();
        matrixStack.scale( scale.x(), scale.y(), scale.z() );

        matrixStack.mulPose( matrix.getRightRotation() );
    }
}
