// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.impl.UpgradeManager;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncs turtle and pocket upgrades to the client.
 */
public final class UpgradesLoadedMessage implements NetworkMessage<ClientNetworkContext> {
    private final Map<String, UpgradeManager.UpgradeWrapper<ITurtleUpgrade>> turtleUpgrades;
    private final Map<String, UpgradeManager.UpgradeWrapper<IPocketUpgrade>> pocketUpgrades;

    public UpgradesLoadedMessage() {
        turtleUpgrades = TurtleUpgrades.instance().getUpgradeWrappers();
        pocketUpgrades = PocketUpgrades.instance().getUpgradeWrappers();
    }

    public UpgradesLoadedMessage(FriendlyByteBuf buf) {
        turtleUpgrades = fromBytes(buf, ITurtleUpgrade.serialiserRegistryKey());
        pocketUpgrades = fromBytes(buf, IPocketUpgrade.serialiserRegistryKey());
    }

    private <T extends UpgradeBase> Map<String, UpgradeManager.UpgradeWrapper<T>> fromBytes(
        FriendlyByteBuf buf, ResourceKey<Registry<UpgradeSerialiser<? extends T>>> registryKey
    ) {
        var registry = RegistryHelper.getRegistry(registryKey);

        var size = buf.readVarInt();
        Map<String, UpgradeManager.UpgradeWrapper<T>> upgrades = new HashMap<>(size);
        for (var i = 0; i < size; i++) {
            var id = buf.readUtf();

            var serialiserId = buf.readResourceLocation();
            var serialiser = registry.get(serialiserId);
            if (serialiser == null) throw new IllegalStateException("Unknown serialiser " + serialiserId);

            var upgrade = serialiser.fromNetwork(new ResourceLocation(id), buf);
            var modId = buf.readUtf();

            upgrades.put(id, new UpgradeManager.UpgradeWrapper<T>(id, upgrade, serialiser, modId));
        }

        return upgrades;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        toBytes(buf, ITurtleUpgrade.serialiserRegistryKey(), turtleUpgrades);
        toBytes(buf, IPocketUpgrade.serialiserRegistryKey(), pocketUpgrades);
    }

    private <T extends UpgradeBase> void toBytes(
        FriendlyByteBuf buf, ResourceKey<Registry<UpgradeSerialiser<? extends T>>> registryKey, Map<String, UpgradeManager.UpgradeWrapper<T>> upgrades
    ) {
        var registry = RegistryHelper.getRegistry(registryKey);

        buf.writeVarInt(upgrades.size());
        for (var entry : upgrades.entrySet()) {
            buf.writeUtf(entry.getKey());

            var serialiser = entry.getValue().serialiser();
            @SuppressWarnings("unchecked")
            var unwrappedSerialiser = (UpgradeSerialiser<T>) serialiser;

            buf.writeResourceLocation(RegistryHelper.getKeyOrThrow(registry, serialiser));
            unwrappedSerialiser.toNetwork(buf, entry.getValue().upgrade());

            buf.writeUtf(entry.getValue().modId());
        }
    }

    @Override
    public void handle(ClientNetworkContext context) {
        TurtleUpgrades.instance().loadFromNetwork(turtleUpgrades);
        PocketUpgrades.instance().loadFromNetwork(pocketUpgrades);
    }

    @Override
    public MessageType<UpgradesLoadedMessage> type() {
        return NetworkMessages.UPGRADES_LOADED;
    }
}
