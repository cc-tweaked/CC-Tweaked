// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.google.auto.service.AutoService;
import net.minecraft.client.resources.model.ModelDebugName;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

@AutoService(ClientPlatformHelper.class)
public class ClientPlatformHelperImpl implements ClientPlatformHelper {
    @Override
    public <T> ModelKey<T> createModelKey(ModelDebugName name) {
        return new ForgeModelKey<>(new StandaloneModelKey<T>(name));
    }
}
