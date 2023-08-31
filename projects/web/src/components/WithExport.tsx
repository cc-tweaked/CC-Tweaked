// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { createElement as h, useContext, createContext, type FunctionComponent, type ReactNode } from "react";

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

export const useExport = () => useContext(DataExport);
export default useExport;

export const WithExport: FunctionComponent<{ data: DataExport, children: ReactNode }> =
    ({ data, children }) => <DataExport.Provider value={data}> {children}</DataExport.Provider >;
