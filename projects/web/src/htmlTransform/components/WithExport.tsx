// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { type FunctionComponent, type VNode, createContext, h } from "preact";
import { useContext } from "preact/hooks";

export type DataExport = {
    readonly itemNames: Record<string, string>,
    readonly recipes: Record<string, Recipe>,
};

export type Recipe = {
    readonly inputs: Array<Array<string>>,
    readonly output: string,
    readonly count: number,
};

const DataExport = createContext<DataExport>({
    itemNames: {},
    recipes: {},
});

export const useExport = (): DataExport => useContext(DataExport);
export default useExport;

export const WithExport: FunctionComponent<{ data: DataExport, children: VNode }> =
    ({ data, children }) => <DataExport.Provider value={data}> {children}</DataExport.Provider>;
