/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ComputerRegistry<TComputer extends IComputer> {
    private Map<Integer, TComputer> m_computers;
    private int m_nextUnusedInstanceID;
    private int m_sessionID;

    protected ComputerRegistry() {
        this.m_computers = new HashMap<>();
        this.reset();
    }

    public void reset() {
        this.m_computers.clear();
        this.m_nextUnusedInstanceID = 0;
        this.m_sessionID = new Random().nextInt();
    }

    public int getSessionID() {
        return this.m_sessionID;
    }

    public int getUnusedInstanceID() {
        return this.m_nextUnusedInstanceID++;
    }

    public Collection<TComputer> getComputers() {
        return this.m_computers.values();
    }

    public TComputer get(int instanceID) {
        if (instanceID >= 0) {
            if (this.m_computers.containsKey(instanceID)) {
                return this.m_computers.get(instanceID);
            }
        }
        return null;
    }

    public boolean contains(int instanceID) {
        return this.m_computers.containsKey(instanceID);
    }

    public void add(int instanceID, TComputer computer) {
        if (this.m_computers.containsKey(instanceID)) {
            this.remove(instanceID);
        }
        this.m_computers.put(instanceID, computer);
        this.m_nextUnusedInstanceID = Math.max(this.m_nextUnusedInstanceID, instanceID + 1);
    }

    public void remove(int instanceID) {
        this.m_computers.remove(instanceID);
    }
}
