/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.shared.computer.core.ServerComputer;

import java.util.function.Supplier;

/**
 * A proxy object for computer objects, delegating to {@link ServerComputer} or {@link TileComputer} where appropriate.
 */
public final class ComputerProxy {
    private final Supplier<TileComputerBase> get;

    public ComputerProxy(Supplier<TileComputerBase> get) {
        this.get = get;
    }

    TileComputerBase getTile() {
        return get.get();
    }

    public void turnOn() {
        var tile = getTile();
        var computer = tile.getServerComputer();
        if (computer == null) {
            tile.startOn = true;
        } else {
            computer.turnOn();
        }
    }

    public void shutdown() {
        var tile = getTile();
        var computer = tile.getServerComputer();
        if (computer == null) {
            tile.startOn = false;
        } else {
            computer.shutdown();
        }
    }

    public void reboot() {
        var tile = getTile();
        var computer = tile.getServerComputer();
        if (computer == null) {
            tile.startOn = true;
        } else {
            computer.reboot();
        }
    }

    public int getID() {
        var tile = getTile();
        var computer = tile.getServerComputer();
        return computer == null ? tile.getComputerID() : computer.getID();
    }

    public boolean isOn() {
        var computer = getTile().getServerComputer();
        return computer != null && computer.isOn();
    }

    public String getLabel() {
        var tile = getTile();
        var computer = tile.getServerComputer();
        return computer == null ? tile.getLabel() : computer.getLabel();
    }
}
