// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Builders;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleToolDurability;
import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftArbitraries;
import dan200.computercraft.test.shared.MinecraftEqualities;
import dan200.computercraft.test.shared.NetworkSupport;
import dan200.computercraft.test.shared.WithMinecraft;

import javax.annotation.Nullable;

import static org.hamcrest.MatcherAssert.assertThat;

@WithMinecraft
class TurtleToolSerialiserTest {
    static {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
    }

    static class TurtleToolBuilder {
        ResourceLocation id;
        String adjective;
        Item craftItem;
        ItemStack toolItem;
        float damageMulitiplier;
        boolean allowEnchantments;
        TurtleToolDurability consumeDurability;
        @Nullable TagKey<Block> breakable;
        String peripheralType;

        public TurtleToolBuilder withId(ResourceLocation id) {
            this.id = id;
            return this;
        }

        public TurtleToolBuilder withAdjective(String adjective) {
            this.adjective = adjective;
            return this;
        }

        public TurtleToolBuilder withCraftItem(Item craftItem) {
            this.craftItem = craftItem;
            return this;
        }

        public TurtleToolBuilder withToolItem(ItemStack toolItem) {
            this.toolItem = toolItem;
            return this;
        }

        public TurtleToolBuilder withDamageMultiplier(float damageMulitiplier) {
            this.damageMulitiplier = damageMulitiplier;
            return this;
        }

        public TurtleToolBuilder withAllowEnchantments(boolean allowEnchantments) {
            this.allowEnchantments = allowEnchantments;
            return this;
        }

        public TurtleToolBuilder withConsumeDurability(TurtleToolDurability consumeDurability) {
            this.consumeDurability = consumeDurability;
            return this;
        }

        public TurtleToolBuilder withBreakable(@Nullable TagKey<Block> breakable) {
            this.breakable = breakable;
            return this;
        }

        public TurtleToolBuilder withPeripheralType(String peripheralType) {
            this.peripheralType = peripheralType;
            return this;
        }

        public TurtleTool build() {
            return new TurtleTool(id, adjective, craftItem, toolItem, damageMulitiplier, allowEnchantments, consumeDurability, breakable, peripheralType);
        }
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

        return Builders.withBuilder(() -> new TurtleToolBuilder())
            .use(MinecraftArbitraries.resourceLocation()).in((builder, resourceLocation) -> builder.withId(resourceLocation))
            .use(Arbitraries.strings().ofMaxLength(100)).in((builder, adjective) -> builder.withAdjective(adjective))
            .use(MinecraftArbitraries.item()).in((builder, craftItem) -> builder.withCraftItem(craftItem))
            .use(MinecraftArbitraries.itemStack()).in((builder, toolItem) -> builder.withToolItem(toolItem))
            .use(Arbitraries.floats()).in((builder, damageMulitiplier) -> builder.withDamageMultiplier(damageMulitiplier))
            .use(Arbitraries.of(true, false)).in((builder, allowEnchantments) -> builder.withAllowEnchantments(allowEnchantments))
            .use(Arbitraries.of(TurtleToolDurability.values())).in((builder, consumeDurability) -> builder.withConsumeDurability(consumeDurability))
            .use(MinecraftArbitraries.tagKey(Registries.BLOCK)).in((builder, breakable) -> builder.withBreakable(breakable))
            .use(Arbitraries.strings().ofMaxLength(100)).in((builder, peripheralType) -> builder.withPeripheralType(peripheralType))
            .build(builder -> builder.build());
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
