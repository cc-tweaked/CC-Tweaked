// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import { type FunctionComponent } from "preact";

/**
 * Wrap a component and ensure that no children are passed to it.
 *
 * Our custom tags *must* be explicitly closed, as <foo /> will be parsed as
 * <foo>(rest of the document)</foo>. This ensures you've not forgotten to do
 * that.
 *
 * @param component The component to wrap
 * @returns A new functional component identical to the previous one
 */
export const noChildren = function <T>(component: FunctionComponent<T>): FunctionComponent<T> {
    // I hope that our few remaining friends
    // Give up on trying to save us

    const name = component.displayName ?? component.name;
    const wrapped: FunctionComponent<T> = props => {
        if (props.children) throw Error("Unexpected children in " + name);

        return component(props);
    };
    wrapped.displayName = name;
    return wrapped;
};
