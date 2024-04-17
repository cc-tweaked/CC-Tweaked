// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { type ComputerActionable, type KeyCode, type LuaValue, Semaphore, TerminalData, lwjgl3Code } from "@squid-dev/cc-web-term";
import { type ComputerDisplay, type ComputerHandle, type PeripheralKind, type Side, start } from "./java";

const colours = "0123456789abcdef";

/**
 * A reference to an emulated computer.
 *
 * This acts as a bridge between the Java-side computer and our Javascript code,
 * including the terminal renderer and the main computer component.
 */
class EmulatedComputer implements ComputerDisplay, ComputerActionable {
    public readonly terminal: TerminalData = new TerminalData();
    public readonly semaphore: Semaphore = new Semaphore();
    private readonly stateChanged: (label: string | null, on: boolean) => void;

    private label: string | null = null;

    private computer?: ComputerHandle;
    private callbacks: Array<(cb: ComputerHandle) => void> = [];
    private removed: boolean = false;

    public constructor(stateChange: (label: string | null, on: boolean) => void) {
        this.stateChanged = stateChange;
        this.label = null;
    }

    public getLabel(): string | null {
        return this.label;
    }

    public setState(label: string | null, on: boolean): void {
        if (this.label !== label) this.label = label;
        this.stateChanged(label, on);
    }

    public updateTerminal(
        width: number, height: number,
        x: number, y: number, blink: boolean, cursorColour: number,
    ): void {
        this.terminal.resize(width, height);
        this.terminal.cursorX = x;
        this.terminal.cursorY = y;
        this.terminal.cursorBlink = blink;
        this.terminal.currentFore = colours.charAt(cursorColour);
    }

    public setTerminalLine(line: number, text: string, fore: string, back: string): void {
        this.terminal.text[line] = text;
        this.terminal.fore[line] = fore;
        this.terminal.back[line] = back;
    }

    public setPaletteColour(colour: number, r: number, g: number, b: number): void {
        this.terminal.palette[colours.charAt(colour)] =
            `rgb(${(r * 0xFF) & 0xFF},${(g * 0xFF) & 0xFF},${(b * 0xFF) & 0xFF})`;
    }

    public flushTerminal(): void {
        this.semaphore.signal();
    }

    public start(): void {
        start(this)
            .then(computer => {
                this.computer = computer;
                if (this.removed) computer.dispose();
                for (const callback of this.callbacks) callback(computer);
            })
            .catch(e => {
                console.error(e);

                if (this.terminal.sizeX === 0 || this.terminal.sizeY === 0) this.terminal.resize(51, 19);

                const width = this.terminal.sizeX;
                const fg = "0".repeat(width);
                const bg = "e".repeat(width);

                const message = `${e}`.replace(/(?![^\n]{1,51}$)([^\n]{1,51})\s/, "$1\n").split("\n");
                for (let y = 0; y < this.terminal.sizeY; y++) {
                    const text = message[y] ?? "";
                    this.terminal.text[y] = text.length > width ? text.substring(0, width) : text + " ".repeat(width - text.length);
                    this.terminal.fore[y] = fg;
                    this.terminal.back[y] = bg;
                }

                this.flushTerminal();
            });
    }

    public queueEvent(event: string, args: Array<LuaValue>): void {
        if (this.computer !== undefined) this.computer.event(event, args);
    }

    public keyDown(key: KeyCode, repeat: boolean): void {
        const code = lwjgl3Code(key);
        if (code !== undefined) this.queueEvent("key", [code, repeat]);
    }

    public keyUp(key: KeyCode): void {
        const code = lwjgl3Code(key);
        if (code !== undefined) this.queueEvent("key_up", [code]);
    }

    public turnOn(): void {
        this.computer?.turnOn();
    }

    public shutdown(): void {
        this.computer?.shutdown();
    }

    public reboot(): void {
        this.computer?.reboot();
    }

    public dispose(): void {
        this.removed = true;
        this.computer?.dispose();
    }

    public transferFiles(files: Array<{ name: string, contents: ArrayBuffer }>): void {
        this.computer?.transferFiles(files);
    }

    public setPeripheral(side: Side, kind: PeripheralKind | null): void {
        if (this.computer) {
            this.computer.setPeripheral(side, kind);
        } else {
            this.callbacks.push(handler => handler.setPeripheral(side, kind));
        }
    }

    public addFile(path: string, contents: string | ArrayBuffer): void {
        if (this.computer) {
            this.computer.addFile(path, contents);
        } else {
            this.callbacks.push(handler => handler.addFile(path, contents));
        }
    }
}

export default EmulatedComputer;
