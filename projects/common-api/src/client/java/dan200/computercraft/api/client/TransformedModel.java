// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0


package dan200.computercraft.api.client;

import com.mojang.math.Transformation;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * A model to render, combined with a transformation matrix to apply.
 */
public final class TransformedModel {
    private final BakedModel model;
    private final Transformation matrix;
    private final @Nullable IntUnaryOperator tints;

    /**
     * Create a new {@link TransformedModel} with a transformation and tinting function.
     *
     * @param model  The model to render.
     * @param matrix The transformation to apply to this model.
     * @param tints  The function mapping tint indexes to colours.
     * @since 1.121.0
     */
    public TransformedModel(BakedModel model, Transformation matrix, @Nullable IntUnaryOperator tints) {
        this.model = Objects.requireNonNull(model);
        this.matrix = Objects.requireNonNull(matrix);
        this.tints = tints;
    }

    public TransformedModel(BakedModel model, Transformation matrix) {
        this(model, matrix, null);
    }

    public TransformedModel(BakedModel model) {
        this(model, Transformation.identity(), null);
    }

    public static TransformedModel of(ModelResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(modelManager.getModel(location));
    }

    public static TransformedModel of(ResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(ClientPlatformHelper.get().getModel(modelManager, location));
    }

    public static TransformedModel of(ItemStack item, Transformation transform) {
        var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        return new TransformedModel(model, transform);
    }

    public BakedModel getModel() {
        return model;
    }

    public Transformation getMatrix() {
        return matrix;
    }

    /**
     * Get the function used to provide tints. This a tint index in the model to a colour.
     *
     * @return The tinting function.
     * @see #getTint(int)
     * @see net.minecraft.client.color.item.ItemColor
     * @since 1.121.0
     */
    public @Nullable IntUnaryOperator getTints() {
        return tints;
    }

    /**
     * Get the tint for a specific tint index.
     *
     * @param index The tint index.
     * @return The colour for this tint index.
     * @see #getTints()
     * @since 1.121.0
     */
    public int getTint(int index) {
        return tints == null ? -1 : tints.applyAsInt(index);
    }
}
