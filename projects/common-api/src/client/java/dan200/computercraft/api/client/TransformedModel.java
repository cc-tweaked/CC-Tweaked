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

import java.util.function.IntUnaryOperator;

/**
 * A model to render, combined with a transformation matrix to apply.
 *
 * @param model  The model.
 * @param matrix The transformation matrix.
 * @param tints  A function mapping tint indexes to a colour.
 */
public record TransformedModel(BakedModel model, Transformation matrix, @Nullable IntUnaryOperator tints) {
    public TransformedModel(BakedModel model) {
        this(model, Transformation.identity(), null);
    }

    public TransformedModel(BakedModel model, Transformation matrix) {
        this(model, matrix, null);
    }

    /**
     * Look up a model in the model bakery and construct a {@link TransformedModel} with no transformation.
     *
     * @param location The location of the model to load.
     * @return The new {@link TransformedModel} instance.
     */
    public static TransformedModel of(ModelLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(location.getModel(modelManager));
    }

    /**
     * Look up a model in the model bakery and construct a {@link TransformedModel} with no transformation.
     *
     * @param location The location of the model to load.
     * @return The new {@link TransformedModel} instance.
     * @see ModelLocation#ofModel(ModelResourceLocation)
     */
    public static TransformedModel of(ModelResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(modelManager.getModel(location));
    }

    /**
     * Look up a model in the model bakery and construct a {@link TransformedModel} with no transformation.
     *
     * @param location The location of the model to load.
     * @return The new {@link TransformedModel} instance.
     * @see ModelLocation#ofResource(ResourceLocation)
     */
    public static TransformedModel of(ResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(ClientPlatformHelper.get().getModel(modelManager, location));
    }

    public static TransformedModel of(ItemStack item, Transformation transform) {
        var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        return new TransformedModel(model, transform);
    }

    /**
     * Get the tint for a specific tint index.
     *
     * @param index The tint index.
     * @return The colour for this tint index.
     * @see #tints()
     * @since 1.121.0
     */
    public int getTint(int index) {
        return tints == null ? -1 : tints.applyAsInt(index);
    }
}
