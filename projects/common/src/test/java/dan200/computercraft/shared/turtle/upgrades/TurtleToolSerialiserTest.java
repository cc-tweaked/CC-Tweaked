// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleToolDurability;
import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftArbitraries;
import dan200.computercraft.test.shared.MinecraftEqualities;
import dan200.computercraft.test.shared.NetworkSupport;
import dan200.computercraft.test.shared.WithMinecraft;
import net.jqwik.api.*;
import net.minecraft.core.registries.Registries;

import static org.hamcrest.MatcherAssert.assertThat;

@WithMinecraft
class TurtleToolSerialiserTest {
    static {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
    }

    /**
     * Sends turtle tools on a roundtrip, ensuring that their contents are reassembled on the other end.
     *
     * @param tool The message to send.
     */
    @Property
    public void testRoundTrip(@ForAll("tool") TurtleTool tool) {
        var converted = NetworkSupport.roundTripSerialiser(
            tool.getUpgradeID(), tool, TurtleToolSerialiser.INSTANCE::toNetwork, TurtleToolSerialiser.INSTANCE::fromNetwork
        );

        if (!equality.equals(tool, converted)) {
            System.out.println("Break");
        }
        assertThat("Messages are equal", converted, equality.asMatcher(TurtleTool.class, tool));
    }

    @Provide
    Arbitrary<TurtleTool> tool() {
        return Combinators.combine(
            MinecraftArbitraries.resourceLocation(),
            Arbitraries.strings().ofMaxLength(100),
            MinecraftArbitraries.item(),
            MinecraftArbitraries.itemStack(),
            Arbitraries.floats(),
            Arbitraries.of(true, false),
            Arbitraries.of(TurtleToolDurability.values()),
            MinecraftArbitraries.tagKey(Registries.BLOCK)
        ).as(TurtleTool::new);
    }

    private static final StructuralEquality<TurtleTool> equality = StructuralEquality.all(
        StructuralEquality.at("id", ITurtleUpgrade::getUpgradeID),
        StructuralEquality.at("craftingItem", ITurtleUpgrade::getCraftingItem, MinecraftEqualities.itemStack),
        StructuralEquality.at("tool", x -> x.item, MinecraftEqualities.itemStack),
        StructuralEquality.at("damageMulitiplier", x -> x.damageMulitiplier),
        StructuralEquality.at("allowEnchantments", x -> x.allowEnchantments),
        StructuralEquality.at("consumeDurability", x -> x.consumeDurability),
        StructuralEquality.at("breakable", x -> x.breakable)
    );
}
