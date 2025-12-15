// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import "setimmediate";

import type { ComputerDisplay, ComputerHandle } from "cct/classes.js";
export type { ComputerDisplay, ComputerHandle, PeripheralKind, Side } from "cct/classes.js";
import { load as teaVMLoad } from "cct/wasm-gc-runtime.js";
import { exceptions, gc } from "wasm-feature-detect";
import wasmClasses from "cct/classes.wasm";

const loadClasses = async (): Promise<{ main: (args: string[]) => void }> => {
    if (
        typeof WebAssembly === "object" && typeof WebAssembly.compileStreaming === "function" &&
        await exceptions() && await gc()
    ) {
        try {
            console.log("Loading WASM runtime");
            return (await teaVMLoad(wasmClasses)).exports;
        } catch (e) {
            console.error("Failed to load WebAssembly runtime", e);
        }
    }

    console.log("Using JS runtime");
    return await import("cct/classes.js");
}

const load = async (): Promise<(computer: ComputerDisplay) => ComputerHandle> => {
    const [classes, { version, resources }] = await Promise.all([loadClasses(), import("cct/resources.js")]);

    let addComputer: ((computer: ComputerDisplay) => ComputerHandle) | null = null;
    const encoder = new TextEncoder();
    window.$javaCallbacks = {
        setup: add => addComputer = add,
        modVersion: version,
        listResources: () => Object.keys(resources),
        getResource: path => new Int8Array(encoder.encode(resources[path]))
    };
    classes.main([]);

    if (!addComputer) throw new Error("Callbacks.setup was never called");
    return addComputer;
};

let addComputer: Promise<(computer: ComputerDisplay) => ComputerHandle> | null = null;

/**
 * Load our emulator and start a new computer.
 *
 * @param computer The display the computer's terminal should be drawn to.
 * @returns The {@link ComputerHandle} for this computer.
 */
export const start = (computer: ComputerDisplay): Promise<ComputerHandle> => {
    if (addComputer == null) addComputer = load();
    return addComputer.then(f => f(computer));
};
