/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.client;

import dan200.computercraft.impl.Services;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
public interface ClientPlatformHelper {
    /**
     * Equivalent to {@link ModelManager#getModel(ModelResourceLocation)} but for arbitrary {@link ResourceLocation}s.
     *
     * @param manager  The model manager.
     * @param location The model location.
     * @return The baked model.
     */
    BakedModel getModel(ModelManager manager, ResourceLocation location);

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
