/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import java.util.function.Supplier;

import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;

/**
 * A proxy object for computer objects, delegating to {@link IComputer} or {@link TileComputer} where appropriate.
 */
public class ComputerProxy {
    private final Supplier<TileComputerBase> get;

    public ComputerProxy(Supplier<TileComputerBase> get) {
        this.get = get;
    }

    public void turnOn() {
        TileComputerBase tile = this.getTile();
        ServerComputer computer = tile.getServerComputer();
        if (computer == null) {
            tile.m_startOn = true;
        } else {
            computer.turnOn();
        }
    }

    protected TileComputerBase getTile() {
        return this.get.get();
    }

    public void shutdown() {
        TileComputerBase tile = this.getTile();
        ServerComputer computer = tile.getServerComputer();
        if (computer == null) {
            tile.m_startOn = false;
        } else {
            computer.shutdown();
        }
    }

    public void reboot() {
        TileComputerBase tile = this.getTile();
        ServerComputer computer = tile.getServerComputer();
        if (computer == null) {
            tile.m_startOn = true;
        } else {
            computer.reboot();
        }
    }

    public int assignID() {
        TileComputerBase tile = this.getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getComputerID() : computer.getID();
    }

    public boolean isOn() {
        ServerComputer computer = this.getTile().getServerComputer();
        return computer != null && computer.isOn();
    }

    public String getLabel() {
        TileComputerBase tile = this.getTile();
        ServerComputer computer = tile.getServerComputer();
        return computer == null ? tile.getLabel() : computer.getLabel();
    }
}
