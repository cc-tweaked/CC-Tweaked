// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import javax.annotation.Nullable;
import java.util.*;

public class ServerComputerRegistry {
    private static final Random RANDOM = new Random();

    private final int sessionId = RANDOM.nextInt();
    private final Map<UUID, ServerComputer> computersByInstanceUuid = new HashMap<>();

    public int getSessionID() {
        return sessionId;
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
        var it = getComputers().iterator();
        while (it.hasNext()) {
            var computer = it.next();
            if (computer.hasTimedOut()) {
                computer.unload();
                computer.onRemoved();
                it.remove();
            } else {
                computer.tickServer();
            }
        }
    }

    void add(ServerComputer computer) {
        var instanceUUID = computer.getInstanceUUID();

        if (computersByInstanceUuid.containsKey(instanceUUID)) {
            throw new IllegalStateException("Duplicate computer " + instanceUUID);
        }

        computersByInstanceUuid.put(instanceUUID, computer);
    }

    void remove(ServerComputer computer) {
        computer.unload();
        computer.onRemoved();
        computersByInstanceUuid.remove(computer.getInstanceUUID());
    }

    void close() {
        for (var computer : getComputers()) computer.unload();
        computersByInstanceUuid.clear();
    }

    public Collection<ServerComputer> getComputers() {
        return computersByInstanceUuid.values();
    }
}
