// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.component;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPIFactory;

/**
 * A component attached to a computer.
 * <p>
 * Components provide a mechanism to attach additional data to a computer, that can then be queried with
 * {@link IComputerSystem#getComponent(ComputerComponent)}.
 * <p>
 * This is largely designed for {@linkplain ILuaAPIFactory custom APIs}, allowing APIs to read additional properties
 * of the computer, such as its position.
 *
 * @param <T> The type of this component.
 * @see ComputerComponents The built-in components.
 */
@SuppressWarnings("UnusedTypeParameter")
public final class ComputerComponent<T> {
    private final String id;

    private ComputerComponent(String id) {
        this.id = id;
    }

    /**
     * Create a new computer component.
     * <p>
     * Mods typically will not need to create their own components.
     *
     * @param namespace The namespace of this component. This should be the mod id.
     * @param id        The unique id of this component.
     * @param <T>       The component
     * @return The newly created component.
     */
    public static <T> ComputerComponent<T> create(String namespace, String id) {
        return new ComputerComponent<>(namespace + ":" + id);
    }

    @Override
    public String toString() {
        return "ComputerComponent(" + id + ")";
    }
}
