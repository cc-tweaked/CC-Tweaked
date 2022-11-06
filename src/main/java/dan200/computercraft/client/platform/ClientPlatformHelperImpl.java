/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

@AutoService(ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    @Override
    public BakedModel getModel(ModelManager manager, ResourceLocation location) {
        return manager.getModel(location);
    }
}
