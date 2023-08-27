// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleToolDurability;
import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftArbitraries;
import dan200.computercraft.test.shared.WithMinecraft;
import io.netty.buffer.Unpooled;
import net.jqwik.api.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.hamcrest.Description;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        TurtleToolSerialiser.INSTANCE.toNetwork(buffer, tool);

        var converted = TurtleToolSerialiser.INSTANCE.fromNetwork(tool.getUpgradeID(), buffer);
        assertEquals(buffer.readableBytes(), 0, "Whole packet was read");

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

    private static final StructuralEquality<ItemStack> stackEquality = new StructuralEquality<>() {
        @Override
        public boolean equals(ItemStack left, ItemStack right) {
            return ItemStack.isSameItemSameTags(left, right) && left.getCount() == right.getCount();
        }

        @Override
        public void describe(Description description, ItemStack object) {
            description.appendValue(object).appendValue(object.getTag());
        }
    };

    private static final StructuralEquality<TurtleTool> equality = StructuralEquality.all(
        StructuralEquality.at("id", ITurtleUpgrade::getUpgradeID),
        StructuralEquality.at("craftingItem", ITurtleUpgrade::getCraftingItem, stackEquality),
        StructuralEquality.at("tool", x -> x.item, stackEquality),
        StructuralEquality.at("damageMulitiplier", x -> x.damageMulitiplier),
        StructuralEquality.at("allowEnchantments", x -> x.allowEnchantments),
        StructuralEquality.at("consumeDurability", x -> x.consumeDurability),
        StructuralEquality.at("breakable", x -> x.breakable)
    );
}
