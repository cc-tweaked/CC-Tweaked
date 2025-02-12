// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.api.component.ComputerComponents;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.network.client.PocketComputerDeletedClientMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * A {@link ServerComputer}-subclass for {@linkplain PocketComputerItem pocket computers}.
 * <p>
 * This extends default {@link ServerComputer} behaviour by also syncing pocket computer state to nearby players, and
 * syncing the terminal to the current player.
 * <p>
 * The actual pocket computer state (upgrade, light) is maintained in {@link PocketBrain}. The two classes are tightly
 * coupled, and maintain a reference to each other.
 *
 * @see PocketComputerDataMessage
 * @see PocketComputerDeletedClientMessage
 */
public final class PocketServerComputer extends ServerComputer {
    private final PocketBrain brain;

    // The state the previous tick, used to determine if the state needs to be sent to the client.
    private int oldLightColour = -1;
    private @Nullable ComputerState oldComputerState;

    private Set<ServerPlayer> tracking = Set.of();

    PocketServerComputer(PocketBrain brain, PocketHolder holder, ServerComputer.Properties properties) {
        super(holder.level(), holder.blockPos(), properties
            .terminalSize(ConfigSpec.pocketTermWidth.get(), ConfigSpec.pocketTermHeight.get())
            .addComponent(ComputerComponents.POCKET, brain)
        );
        this.brain = brain;
    }

    public PocketBrain getBrain() {
        return brain;
    }

    @Override
    protected void tickServer() {
        super.tickServer();

        // Get the new set of players tracking the current position.
        var newTracking = getLevel().getChunkSource().chunkMap.getPlayers(new ChunkPos(getPosition()), false);
        var trackingChanged = tracking.size() != newTracking.size() || !tracking.containsAll(newTracking);

        // And now find any new players, add them to the tracking list, and broadcast state where appropriate.
        var state = getState();
        var light = brain.getLight();
        if (oldLightColour != light || oldComputerState != state) {
            oldComputerState = state;
            oldLightColour = light;

            // Broadcast the state to all players
            ServerNetworking.sendToPlayers(new PocketComputerDataMessage(this, false), newTracking);
        } else if (trackingChanged) {
            // Broadcast the state to new players.
            var added = newTracking.stream().filter(x -> !tracking.contains(x)).toList();
            if (!added.isEmpty()) {
                ServerNetworking.sendToPlayers(new PocketComputerDataMessage(this, brain.holder().isTerminalAlwaysVisible()), added);
            }
        }

        if (trackingChanged) tracking = Set.copyOf(newTracking);
    }

    @Override
    protected void onTerminalChanged() {
        super.onTerminalChanged();

        var holder = brain.holder() instanceof PocketHolder.PlayerHolder h && h.isValid(this) ? h.entity() : null;
        if (brain.holder().isTerminalAlwaysVisible() && !tracking.isEmpty()) {
            // If the terminal is always visible, send it to all players *and* the holder.
            var packet = new PocketComputerDataMessage(this, true);
            ServerNetworking.sendToPlayers(packet, tracking);
            if (holder != null && !tracking.contains(holder)) ServerNetworking.sendToPlayer(packet, holder);
        } else if (holder != null) {
            // Otherwise just send it to the holder.
            ServerNetworking.sendToPlayer(new PocketComputerDataMessage(this, true), holder);
        }
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        ServerNetworking.sendToAllPlayers(new PocketComputerDeletedClientMessage(getInstanceUUID()), getLevel().getServer());
    }
}
