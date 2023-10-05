// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { type FunctionalComponent, h } from "preact";
import type { PeripheralKind, Side } from "./java";
import { useEffect, useMemo, useState } from "preact/hooks";
import { Terminal } from "@squid-dev/cc-web-term";
import EmulatedComputer from "./computer";
import termFont from "@squid-dev/cc-web-term/assets/term_font.png";

export type ComputerProps = {
    files?: Record<string, string | ArrayBuffer>,
    peripherals?: Partial<Record<Side, PeripheralKind | null>>,
}

/**
 * Renders a computer in the world.
 *
 * @param props The properties for this component
 * @returns The resulting JSX element.
 */
const Computer: FunctionalComponent<ComputerProps> = ({ files, peripherals }) => {
    const [label, setLabel] = useState<string | null>(null);
    const [isOn, setOn] = useState(false);
    const computer = useMemo(() => {
        const computer = new EmulatedComputer((label, on) => {
            setLabel(label);
            setOn(on);
        });
        for (const [side, peripheral] of Object.entries(peripherals ?? {})) {
            computer.setPeripheral(side as Side, peripheral);
        }
        for (const [path, contents] of Object.entries(files ?? {})) {
            computer.addFile(path, contents);
        }
        return computer;
    }, [setLabel, setOn, files, peripherals]);

    useEffect(() => {
        computer.start();
        return () => computer.dispose();
    }, [computer]);

    return <Terminal
        id={0} label={label} on={isOn} focused={true} font={termFont}
        computer={computer} terminal={computer.terminal} changed={computer.semaphore}
    />;
};

export default Computer;
