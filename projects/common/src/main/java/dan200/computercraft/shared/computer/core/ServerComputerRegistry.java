// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

public class ServerComputerRegistry {
    private static final Random RANDOM = new Random();

    private final int sessionId = RANDOM.nextInt();
    private final Int2ObjectMap<ServerComputer> computers = new Int2ObjectOpenHashMap<>();
    private int nextInstanceId;

    public int getSessionID() {
        return sessionId;
    }

    int getUnusedInstanceID() {
        return nextInstanceId++;
    }

    @Nullable
    public ServerComputer get(int instanceID) {
        return instanceID >= 0 ? computers.get(instanceID) : null;
    }

    @Nullable
    public ServerComputer get(int sessionId, int instanceId) {
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

    void add(int instanceID, ServerComputer computer) {
        remove(instanceID);
        computers.put(instanceID, computer);
        nextInstanceId = Math.max(nextInstanceId, instanceID + 1);
    }

    void remove(int instanceID) {
        var computer = get(instanceID);
        if (computer != null) {
            computer.unload();
            computer.onRemoved();
        }

        computers.remove(instanceID);
    }

    void close() {
        for (var computer : getComputers()) computer.unload();
        computers.clear();
    }

    public Collection<ServerComputer> getComputers() {
        return computers.values();
    }
}
