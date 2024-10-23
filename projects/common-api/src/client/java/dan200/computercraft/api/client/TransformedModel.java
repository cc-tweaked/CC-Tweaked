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

/**
 * A model to render, combined with a transformation matrix to apply.
 *
 * @param model  The model.
 * @param matrix The transformation matrix.
 */
public record TransformedModel(BakedModel model, Transformation matrix) {
    public TransformedModel(BakedModel model) {
        this(model, Transformation.identity());
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
        var model = Minecraft.getInstance().getItemRenderer().getModel(item, null, null, 0);
        return new TransformedModel(model, transform);
    }
}
