// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import cc.tweaked.web.js.ComputerDisplay;
import cc.tweaked.web.js.ComputerHandle;
import cc.tweaked.web.js.JavascriptConv;
import cc.tweaked.web.peripheral.SpeakerPeripheral;
import cc.tweaked.web.peripheral.TickablePeripheral;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.transfer.TransferredFile;
import dan200.computercraft.core.apis.transfer.TransferredFiles;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerEnvironment;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.filesystem.MemoryMount;
import dan200.computercraft.core.metrics.MetricsObserver;
import dan200.computercraft.core.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Manages the core lifecycle of an emulated {@link Computer}.
 * <p>
 * This is exposed to Javascript via the {@link ComputerHandle} interface.
 */
class EmulatedComputer implements ComputerEnvironment, ComputerHandle {
    private static final Logger LOG = LoggerFactory.getLogger(EmulatedComputer.class);

    private static final ComputerSide[] SIDES = ComputerSide.values();
    private boolean terminalChanged = false;
    private final Terminal terminal = new Terminal(51, 19, true, () -> terminalChanged = true);
    private final Computer computer;
    private final ComputerDisplay computerAccess;
    private boolean disposed = false;
    private final MemoryMount mount = new MemoryMount();

    private @Nullable String oldLabel;
    private boolean oldOn;

    EmulatedComputer(ComputerContext context, ComputerDisplay computerAccess) {
        this.computerAccess = computerAccess;
        this.computer = new Computer(context, this, terminal, 0);

        if (!disposed) computer.turnOn();
    }

    /**
     * Tick this computer.
     *
     * @return If this computer has been disposed of.
     */
    public boolean tick() {
        if (disposed && computer.isOn()) computer.unload();

        try {
            computer.tick();
        } catch (RuntimeException e) {
            LOG.error("Error when ticking computer", e);
        }

        var newLabel = computer.getLabel();
        var newOn = computer.isOn();
        if (!Objects.equals(oldLabel, newLabel) || oldOn != newOn) {
            computerAccess.setState(oldLabel = newLabel, oldOn = newOn);
        }

        for (var side : SIDES) {
            var peripheral = computer.getEnvironment().getPeripheral(side);
            if (peripheral instanceof TickablePeripheral toTick) toTick.tick();
        }

        if (terminalChanged) {
            terminalChanged = false;
            computerAccess.updateTerminal(
                terminal.getWidth(), terminal.getHeight(),
                terminal.getCursorX(), terminal.getCursorY(),
                terminal.getCursorBlink(), terminal.getTextColour()
            );

            for (var i = 0; i < terminal.getHeight(); i++) {
                computerAccess.setTerminalLine(i,
                    terminal.getLine(i).toString(),
                    terminal.getTextColourLine(i).toString(),
                    terminal.getBackgroundColourLine(i).toString()
                );
            }

            var palette = terminal.getPalette();
            for (var i = 0; i < 16; i++) {
                var colours = palette.getColour(i);
                computerAccess.setPaletteColour(15 - i, colours[0], colours[1], colours[2]);
            }

            computerAccess.flushTerminal();
        }

        return disposed && !computer.isOn();
    }

    @Override
    public int getDay() {
        return (int) ((Main.getTicks() + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay() {
        return ((Main.getTicks() + 6000) % 24000) / 1000.0;
    }

    @Nullable
    @Override
    public WritableMount createRootMount() {
        return mount;
    }

    @Override
    public MetricsObserver getMetrics() {
        return MetricsObserver.discard();
    }

    @Override
    public void event(String event, @Nullable JSObject[] args) {
        computer.queueEvent(event, JavascriptConv.toJava(args));
    }

    @Override
    public void shutdown() {
        computer.shutdown();
    }

    @Override
    public void turnOn() {
        computer.turnOn();
    }

    @Override
    public void reboot() {
        computer.reboot();
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public void transferFiles(FileContents[] files) {
        computer.queueEvent(TransferredFiles.EVENT, new Object[]{ new TransferredFiles(
            Arrays.stream(files)
                .map(x -> new TransferredFile(x.getName(), new ArrayByteChannel(bytesOfBuffer(x.getContents()))))
                .toList()
        ) });
    }

    @Override
    public void setPeripheral(String sideName, @Nullable String kind) {
        var side = ComputerSide.valueOfInsensitive(sideName);
        if (side == null) throw new IllegalArgumentException("Unknown sideName");

        IPeripheral peripheral;
        if (kind == null) {
            peripheral = null;
        } else if (kind.equals("speaker")) {
            peripheral = new SpeakerPeripheral();
        } else {
            throw new IllegalArgumentException("Unknown peripheral kind");
        }

        computer.getEnvironment().setPeripheral(side, peripheral);
    }

    @Override
    public void addFile(String path, JSObject contents) {
        byte[] bytes;
        if (JavascriptConv.isArrayBuffer(contents)) {
            bytes = bytesOfBuffer(contents.cast());
        } else {
            JSString string = contents.cast();
            bytes = string.stringValue().getBytes(StandardCharsets.UTF_8);
        }

        mount.addFile(path, bytes);
    }

    private byte[] bytesOfBuffer(ArrayBuffer buffer) {
        var oldBytes = JavascriptConv.asByteArray(buffer);
        return Arrays.copyOf(oldBytes, oldBytes.length);
    }
}
