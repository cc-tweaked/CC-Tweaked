// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.modem.wireless;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NFCNetworkManager {
    private final Map<Entity, WirelessNetwork> networks = new ConcurrentHashMap<>();

    public WirelessNetwork addNetwork(Entity entity) {
        if (!networks.containsKey(entity)) {
            networks.put(entity, new WirelessNetwork(() -> networks.remove(entity)));
        }
        return networks.get(entity);
    }

    public void removeNetwork(Entity entity) {
        networks.remove(entity);
    }

    public @Nullable WirelessNetwork get(Entity entity) {
        return networks.get(entity);
    }
}
