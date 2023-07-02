// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration.libmultipart;

import alexiil.mc.lib.attributes.*;
import alexiil.mc.lib.multipart.api.NativeMultipart;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.node.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.libmultipart.parts.WirelessModemPart;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration for <a href="https://github.com/AlexIIL/LibMultiPart/">LibMultiPart</a>.
 * <p>
 * This adds multipart versions of modems and cables.
 */
public final class LibMultiPartIntegration {
    public static final String MOD_ID = "libmultipart";

    public static final Attribute<IPeripheral> PERIPHERAL = Attributes.create(IPeripheral.class);
    public static final Attribute<WiredElement> WIRED_ELEMENT = Attributes.create(WiredElement.class);

    private static final Map<Item, PlacementMultipartCreator> itemPlacers = new HashMap<>();

    private LibMultiPartIntegration() {
    }

    public static void init() {
        // Register an adapter from Fabric block lookup to attributes. This would be very inefficient by default, so
        // we only do it for blocks which explicitly implement the attribute interfaces.
        PeripheralLookup.get().registerFallback((world, pos, state, blockEntity, context) ->
            state.getBlock() instanceof AttributeProvider || blockEntity instanceof AttributeProviderBlockEntity
                ? PERIPHERAL.getFirstOrNull(world, pos, SearchOptions.inDirection(context.getOpposite()))
                : null);

        WiredElementLookup.get().registerFallback((world, pos, state, blockEntity, context) ->
            state.getBlock() instanceof AttributeProvider || blockEntity instanceof AttributeProviderBlockEntity
                ? WIRED_ELEMENT.getFirstOrNull(world, pos, SearchOptions.inDirection(context.getOpposite()))
                : null);

        registerWirelessModem(WirelessModemPart.makeDefinition(ModRegistry.Blocks.WIRELESS_MODEM_NORMAL, false));
        registerWirelessModem(WirelessModemPart.makeDefinition(ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED, true));
    }

    private static void registerWirelessModem(WirelessModemPart.Definition definition) {
        definition.register();

        NativeMultipart.LOOKUP.registerForBlocks((world, pos, state, blockEntity, context) -> (level, blockPos, blockState) ->
            List.of(holder -> definition.convert(holder, state, blockEntity)), definition.block());

        itemPlacers.put(definition.block().asItem(), definition);
    }

    /**
     * Get the corresponding {@link PlacementMultipartCreator} for an item.
     *
     * @param item The item we're trying to place.
     * @return The placement-aware multipart creator, or {@code null}.
     */
    public static @Nullable PlacementMultipartCreator getCreatorForItem(Item item) {
        return itemPlacers.get(item);
    }
}
