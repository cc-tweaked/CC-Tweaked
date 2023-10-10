// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import "setimmediate";

import type { ComputerDisplay, ComputerHandle } from "cct/classes";
export type { ComputerDisplay, ComputerHandle, PeripheralKind, Side } from "cct/classes";

const load = async (): Promise<(computer: ComputerDisplay) => ComputerHandle> => {
    const [classes, { version, resources }] = await Promise.all([import("cct/classes"), import("cct/resources")]);

    let addComputer: ((computer: ComputerDisplay) => ComputerHandle) | null = null;
    const encoder = new TextEncoder();
    window.$javaCallbacks = {
        setup: add => addComputer = add,
        modVersion: version,
        listResources: () => Object.keys(resources),
        getResource: path => new Int8Array(encoder.encode(resources[path]))
    };
    classes.main();

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
