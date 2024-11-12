// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL
package dan200.computercraft.core.redstone;

import dan200.computercraft.core.computer.ComputerSide;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the state of redstone inputs and ouputs on a computer (or other redstone emitting block).
 * <p>
 * As computers execute on a separate thread to the main Minecraft world, computers cannot immediately read or write
 * redstone values. Instead, we maintain a copy of the block's redstone inputs and outputs, and sync that with the
 * Minecraft world when needed.
 *
 * <h2>Input</h2>
 * Redstone inputs should be propagated immediately to the internal state of the computer. Computers (and other redstone
 * blocks) listen for block updates, fetch their neighbour's redstone state, and then call
 * {@link #setInput(ComputerSide, int, int)}.
 * <p>
 * However, we do not want to immediately schedule a {@code "redstone"} event, as otherwise we could schedule many
 * events in a single tick. Instead, the next time the block is ticked, the consumer should call
 * {@link #pollInputChanged()} and queue an event if needed.
 *
 * <h2>Output</h2>
 * In order to reduce block updates, we maintain a separate "internal" and "external" output state. Whenever a computer
 * sets a redstone output, the "internal" state is updated, and a dirty flag is set. When the computer is ticked,
 * {@link #updateOutput()} should be called, to copy the internal state to the external state. This returns a bitmask
 * indicating which sides have changed. The external outputs may then be read with {@link #getExternalOutput(ComputerSide)}
 * and {@link #getExternalBundledOutput(ComputerSide)}.
 */
public final class RedstoneState implements RedstoneAccess {
    private final @Nullable Runnable onOutputChanged;

    private final ReentrantLock outputLock = new ReentrantLock();
    private @GuardedBy("outputLock") boolean internalOutputChanged = false;
    private final @GuardedBy("outputLock") int[] internalOutput = new int[ComputerSide.COUNT];
    private final @GuardedBy("outputLock") int[] internalBundledOutput = new int[ComputerSide.COUNT];

    private final int[] externalOutput = new int[ComputerSide.COUNT];
    private final int[] externalBundledOutput = new int[ComputerSide.COUNT];

    private final ReentrantLock inputLock = new ReentrantLock();
    private boolean inputChanged = false;
    private final @GuardedBy("inputLock") int[] input = new int[ComputerSide.COUNT];
    private final @GuardedBy("inputLock") int[] bundledInput = new int[ComputerSide.COUNT];

    public RedstoneState() {
        this(null);
    }

    /**
     * Construct a new {@link RedstoneState}, with a callback function to invoke when the <em>internal</em> output has
     * changed. This function is called from the computer thread.
     *
     * @param outputChanged The function to invoke when output has changed.
     */
    public RedstoneState(@Nullable Runnable outputChanged) {
        this.onOutputChanged = outputChanged;
    }

    @Override
    public int getInput(ComputerSide side) {
        inputLock.lock();
        try {
            return input[side.ordinal()];
        } finally {
            inputLock.unlock();
        }
    }

    @Override
    public int getBundledInput(ComputerSide side) {
        inputLock.lock();
        try {
            return bundledInput[side.ordinal()];
        } finally {
            inputLock.unlock();
        }
    }

    @Override
    public void setOutput(ComputerSide side, int output) {
        var index = side.ordinal();

        outputLock.lock();
        try {
            if (internalOutput[index] == output) return;
            internalOutput[index] = output;
            setOutputChanged();
        } finally {
            outputLock.unlock();
        }
    }

    @Override
    public int getOutput(ComputerSide side) {
        outputLock.lock();
        try {
            return internalOutput[side.ordinal()];
        } finally {
            outputLock.unlock();
        }
    }

    @Override
    public void setBundledOutput(ComputerSide side, int output) {
        var index = side.ordinal();
        outputLock.lock();
        try {
            if (internalBundledOutput[index] == output) return;
            internalBundledOutput[index] = output;
            setOutputChanged();
        } finally {
            outputLock.unlock();
        }
    }

    @Override
    public int getBundledOutput(ComputerSide side) {
        outputLock.lock();
        try {
            return internalBundledOutput[side.ordinal()];
        } finally {
            outputLock.unlock();
        }
    }

    @GuardedBy("outputLock")
    private void setOutputChanged() {
        if (internalOutputChanged) return;
        internalOutputChanged = true;
        if (onOutputChanged != null) onOutputChanged.run();
    }

    /**
     * Propagate redstone changes from the computer to the outside world. The effective outputs can be acquired with
     * {@link #getExternalOutput(ComputerSide)} and {@link #getExternalBundledOutput(ComputerSide)}.
     *
     * @return A bitmask indicating which sides have changed (indexed via {@link ComputerSide#ordinal()}).
     */
    public int updateOutput() {
        outputLock.lock();
        try {
            if (!internalOutputChanged) return 0;

            var changed = 0;

            for (var i = 0; i < ComputerSide.COUNT; i++) {
                if (externalOutput[i] != internalOutput[i]) {
                    externalOutput[i] = internalOutput[i];
                    changed |= 1 << i;
                }

                if (externalBundledOutput[i] != internalBundledOutput[i]) {
                    externalBundledOutput[i] = internalBundledOutput[i];
                    changed |= 1 << i;
                }
            }

            internalOutputChanged = false;

            return changed;
        } finally {
            outputLock.unlock();
        }
    }

    /**
     * Get the redstone output for a given side.
     *
     * @param side The side to get.
     * @return The effective redstone output.
     */
    public int getExternalOutput(ComputerSide side) {
        return externalOutput[side.ordinal()];
    }

    /**
     * Get the bundled redstone output for a given side.
     *
     * @param side The side to get.
     * @return The effective bundled redstone output.
     */
    public int getExternalBundledOutput(ComputerSide side) {
        return externalBundledOutput[side.ordinal()];
    }

    /**
     * Reset any redstone output set by the computer.
     */
    public void clearOutput() {
        outputLock.lock();
        try {
            Arrays.fill(internalOutput, 0);
            Arrays.fill(internalBundledOutput, 0);
            internalOutputChanged = true;
        } finally {
            outputLock.unlock();
        }
    }

    /**
     * Set the redstone input for a given side.
     *
     * @param side         The side to update.
     * @param level        The redstone level.
     * @param bundledState The bundled redstone state.
     * @return Whether the input has changed.
     */
    public boolean setInput(ComputerSide side, int level, int bundledState) {
        var index = side.ordinal();
        inputLock.lock();
        try {
            var changed = false;
            if (input[index] != level) {
                input[index] = level;
                changed = true;
            }

            if (bundledInput[index] != bundledState) {
                bundledInput[index] = bundledState;
                changed = true;
            }

            inputChanged |= changed;
            return changed;
        } finally {
            inputLock.unlock();
        }
    }

    /**
     * Check whether any redstone inputs set by {@link #setInput(ComputerSide, int, int)} have changed since the last
     * call to this function.
     *
     * @return Whether any redstone inputs has changed.
     */
    public boolean pollInputChanged() {
        var changed = inputChanged;
        inputChanged = false;
        return changed;
    }
}
