// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import dan200.computercraft.api.client.ModelLocation;
import dan200.computercraft.impl.Services;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
public interface ClientPlatformHelper {
    /**
     * Get a model from a resource.
     *
     * @param manager          The model manager.
     * @param resourceLocation The model resourceLocation.
     * @return The baked model.
     * @see ModelLocation
     */
    BakedModel getModel(ModelManager manager, ResourceLocation resourceLocation);

    /**
     * Set a model from a {@link ModelResourceLocation} or {@link ResourceLocation}.
     * <p>
     * This is largely equivalent to {@code resourceLocation == null ? manager.getModel(modelLocation) : getModel(manager, resourceLocation)},
     * but allows pre-computing {@code modelLocation} (if needed).
     *
     * @param manager          The model manager.
     * @param modelLocation    The location of the model to load.
     * @param resourceLocation The location of the resource, if trying to load from a resource.
     * @return The baked model.
     * @see ModelLocation
     */
    BakedModel getModel(ModelManager manager, ModelResourceLocation modelLocation, @Nullable ResourceLocation resourceLocation);

    /**
     * Wrap this model in a version which renders a foil/enchantment glint.
     *
     * @param model The model to wrap.
     * @return The wrapped model.
     * @see RenderType#glint()
     */
    BakedModel createdFoiledModel(BakedModel model);

    static ClientPlatformHelper get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ClientPlatformHelper.class, Instance.ERROR) : instance;
    }

    final class Instance {
        static final @Nullable ClientPlatformHelper INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ClientPlatformHelper.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
