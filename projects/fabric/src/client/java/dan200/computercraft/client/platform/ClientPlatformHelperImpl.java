// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.resources.model.ModelDebugName;

@AutoService(ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    @Override
    public <T> ModelKey<T> createModelKey(ModelDebugName name) {
        return new FabricModelKey<>(ExtraModelKey.create(name::debugName));
    }
}
