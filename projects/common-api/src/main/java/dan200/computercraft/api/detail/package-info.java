// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/**
 * The detail system provides a standard way for mods to return descriptions of common game objects, such as blocks or
 * items, as well as registering additional detail to be included in those descriptions.
 * <p>
 * For instance, the built-in {@code turtle.getItemDetail()} method uses
 * {@linkplain dan200.computercraft.api.detail.VanillaDetailRegistries#ITEM_STACK in order to provide information about}
 * the selected item:
 *
 * <pre class="language language-lua">{@code
 * local item = turtle.getItemDetail(nil, true)
 * --[[
 * item = {
 *   name = "minecraft:wheat",
 *   displayName = "Wheat",
 *   count = 1,
 *   maxCount = 64,
 *   tags = {},
 * }
 * ]]
 * }</pre>
 *
 * <h2>Built-in detail providers</h2>
 * While you can define your own detail providers (perhaps for types from your own mod), CC comes with several built-in
 * detail registries for vanilla and mod-loader objects:
 *
 * <ul>
 *     <li>{@link dan200.computercraft.api.detail.VanillaDetailRegistries}, for vanilla objects</li>
 *     <li>{@code dan200.computercraft.api.detail.ForgeDetailRegistries} for Forge-specific objects</li>
 *     <li>{@code dan200.computercraft.api.detail.FabricDetailRegistries} for Fabric-specific objects</li>
 * </ul>
 *
 * <h2>Example: Returning details from methods</h2>
 * Here we define a {@code getHeldItem()} method for pocket computers which finds the currently held item of the player
 * and returns it to the user using {@link dan200.computercraft.api.detail.VanillaDetailRegistries#ITEM_STACK} and
 * {@link dan200.computercraft.api.detail.DetailRegistry#getDetails(net.minecraft.core.HolderLookup.Provider, Object)}.
 *
 * {@snippet class=com.example.examplemod.ExamplePocketPeripheral region=details}
 *
 * <h2>Example: Registering custom detail registries</h2>
 * Here we define a new detail provider for items that includes the nutrition and saturation values in the returned object.
 *
 * {@snippet class=com.example.examplemod.ExampleMod region=details}
 */
package dan200.computercraft.api.detail;
