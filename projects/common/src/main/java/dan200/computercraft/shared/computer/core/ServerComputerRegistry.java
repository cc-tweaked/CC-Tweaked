// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.*;

public class ServerComputerRegistry {
    private static final Random RANDOM = new Random();

    private final int sessionId = RANDOM.nextInt();
    private final Int2ObjectMap<ServerComputer> computersByInstanceId = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, ServerComputer> computersByInstanceUuid = new HashMap<>();
    private int nextInstanceId;

    public int getSessionID() {
        return sessionId;
    }

    int getUnusedInstanceID() {
        return nextInstanceId++;
    }

    @Nullable
    public ServerComputer get(int instanceID) {
        return instanceID >= 0 ? computersByInstanceId.get(instanceID) : null;
    }

    @Nullable
    public ServerComputer get(@Nullable UUID instanceID) {
        return instanceID != null ? computersByInstanceUuid.get(instanceID) : null;
    }

    @Nullable
    public ServerComputer get(int sessionId, @Nullable UUID instanceId) {
        return sessionId == this.sessionId ? get(instanceId) : null;
    }

    void update() {
        var it = computersByInstanceId.values().iterator();
        while (it.hasNext()) {
            var computer = it.next();
            if (computer.hasTimedOut()) {
                computer.unload();
                computer.onRemoved();
                it.remove();
                computersByInstanceUuid.remove(computer.getInstanceUUID());
            } else {
                computer.tickServer();
            }
        }
    }

    void add(ServerComputer computer) {
        var instanceID = computer.getInstanceID();
        var instanceUUID = computer.getInstanceUUID();

        if (computersByInstanceId.containsKey(instanceID)) {
            throw new IllegalStateException("Duplicate computer " + instanceID);
        }

        if (computersByInstanceUuid.containsKey(instanceUUID)) {
            throw new IllegalStateException("Duplicate computer " + instanceUUID);
        }

        computersByInstanceId.put(instanceID, computer);
        computersByInstanceUuid.put(instanceUUID, computer);
    }

    void remove(ServerComputer computer) {
        computer.unload();
        computer.onRemoved();
        computersByInstanceId.remove(computer.getInstanceID());
        computersByInstanceUuid.remove(computer.getInstanceUUID());
    }

    void close() {
        for (var computer : getComputers()) computer.unload();
        computersByInstanceId.clear();
        computersByInstanceUuid.clear();
    }

    public Collection<ServerComputer> getComputers() {
        return computersByInstanceId.values();
    }
}
