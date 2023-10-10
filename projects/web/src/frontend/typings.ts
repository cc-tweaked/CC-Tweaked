// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

declare module "*.lua" {
    const contents: string;
    export default contents;
}

declare module "*.nfp" {
    const contents: string;
    export default contents;
}

declare module "*.nft" {
    const contents: string;
    export default contents;
}

declare module "*.settings" {
    const contents: string;
    export default contents;
}

declare module "*.license" {
    const contents: string;
    export default contents;
}

declare module "*.dfpwm" {
    const contents: string;
    export default contents;
}

declare module "cct/resources" {
    export const version: string;
    export const resources: Record<string, string>;
}

declare module "cct/classes" {
    export const main: () => void;

    export type Side = "up" | "down" | "left" | "right" | "front" | "back";
    export type PeripheralKind = "speaker";

    /**
     * Controls a specific computer on the Javascript side. See {@code js/ComputerAccess.java}.
     */
    export interface ComputerDisplay {
        /**
         * Set this computer's current state
         *
         * @param label This computer's label
         * @param on    If this computer is on right now
         */
        setState(label: string | null, on: boolean): void;

        /**
         * Update the terminal's properties
         *
         * @param width        The terminal width
         * @param height       The terminal height
         * @param x            The X cursor
         * @param y            The Y cursor
         * @param blink        Whether the cursor is blinking
         * @param cursorColour The cursor's colour
         */
        updateTerminal(width: number, height: number, x: number, y: number, blink: boolean, cursorColour: number): void;

        /**
         * Set a line on the terminal
         *
         * @param line The line index to set
         * @param text The line's text
         * @param fore The line's foreground
         * @param back The line's background
         */
        setTerminalLine(line: number, text: string, fore: string, back: string): void;

        /**
         * Set the palette colour for a specific index
         *
         * @param colour The colour index to set
         * @param r      The red value, between 0 and 1
         * @param g      The green value, between 0 and 1
         * @param b      The blue value, between 0 and 1
         */
        setPaletteColour(colour: number, r: number, g: number, b: number): void;

        /**
         * Mark the terminal as having changed. Should be called after all other terminal methods.
         */
        flushTerminal(): void;
    }

    export interface ComputerHandle {
        /**
         * Queue an event on the computer.
         */
        event(event: string, args: Array<unknown> | null): void;

        /**
         * Shut the computer down.
         */
        shutdown(): void;

        /**
         * Turn the computer on.
         */
        turnOn(): void;

        /**
         * Reboot the computer.
         */
        reboot(): void;

        /**
         * Dispose of this computer, marking it as no longer running.
         */
        dispose(): void;

        /**
         * Transfer some files to this computer.
         *
         * @param files A list of files and their contents.
         */
        transferFiles(files: Array<{ name: string, contents: ArrayBuffer }>): void;

        /**
         * Set a peripheral on a particular side
         *
         * @param side The side to set the peripheral on.
         * @param kind The kind of peripheral. For now, can only be "speaker".
         */
        setPeripheral(side: Side, kind: PeripheralKind | null): void;

        /**
         * Add a file to this computer's filesystem.
         * @param path     The path of the file.
         * @param contents The path to the file.
         */
        addFile(path: string, contents: string | ArrayBuffer): void;
    }

    export interface Callbacks {
        /**
         * Get the current callback instance
         *
         * @param addComputer A computer to add a new computer.
         */
        setup(addComputer: (computer: ComputerDisplay) => ComputerHandle): void;

        /**
         * The version of CC: Tweaked currently loaded.
         */
        modVersion: string;

        /**
         * List all resources available in the ROM.
         */
        listResources(): Array<string>;

        /**
         * Load a resource from the ROM.
         *
         * @param path The path to the resource to load.
         */
        getResource(path: string): Int8Array;
    }
}

declare namespace JSX {
    export type Element = import("preact").JSX.Element;
    export type IntrinsicElements = import("preact").JSX.IntrinsicElements;
    export type ElementClass = import("preact").JSX.ElementClass;
}

declare var $javaCallbacks: import("cct/classes").Callbacks; // eslint-disable-line no-var
