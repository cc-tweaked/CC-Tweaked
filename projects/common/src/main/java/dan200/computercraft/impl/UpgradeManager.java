// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.util.SafeDispatchCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Manages turtle and pocket computer upgrades.
 *
 * @param <T> The type of upgrade.
 * @see TurtleUpgrades
 * @see PocketUpgrades
 */
public final class UpgradeManager<T extends UpgradeBase> {
    private final ResourceKey<Registry<T>> registry;
    private final Codec<T> upgradeCodec;
    private final Codec<UpgradeData<T>> dataCodec;
    private final StreamCodec<RegistryFriendlyByteBuf, UpgradeData<T>> dataStreamCodec;

    UpgradeManager(
        ResourceKey<Registry<UpgradeType<? extends T>>> typeRegistry,
        ResourceKey<Registry<T>> registry,
        Function<T, UpgradeType<? extends T>> getType
    ) {
        this.registry = registry;

        upgradeCodec = SafeDispatchCodec.ofRegistry(typeRegistry, getType, UpgradeType::codec);

        var holderCodec = RegistryFixedCodec.create(registry).xmap(x -> (Holder.Reference<T>) x, x -> x);
        Codec<UpgradeData<T>> fullCodec = RecordCodecBuilder.create(i -> i.group(
            holderCodec.fieldOf("id").forGetter(UpgradeData::holder),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(UpgradeData::data)
        ).apply(i, UpgradeData::new));
        dataCodec = Codec.withAlternative(fullCodec, holderCodec, UpgradeData::ofDefault);

        dataStreamCodec = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(registry).map(x -> (Holder.Reference<T>) x, x -> x), UpgradeData::holder,
            DataComponentPatch.STREAM_CODEC, UpgradeData::data,
            UpgradeData::new
        );
    }

    /**
     * The codec for an upgrade instance.
     *
     * @return The instance codec.
     */
    public Codec<T> upgradeCodec() {
        return upgradeCodec;
    }

    /**
     * The codec for an upgrade and its associated data.
     *
     * @return The upgrade data codec.
     */
    public Codec<UpgradeData<T>> upgradeDataCodec() {
        return dataCodec;
    }

    /**
     * The stream codec for an upgrade and its associated data.
     *
     * @return The upgrade data codec.
     */
    public StreamCodec<RegistryFriendlyByteBuf, UpgradeData<T>> upgradeDataStreamCodec() {
        return dataStreamCodec;
    }

    public String getOwner(Holder.Reference<T> upgrade) {
        var ns = upgrade.key().identifier().getNamespace();
        return ns.equals("minecraft") ? ComputerCraftAPI.MOD_ID : ns;

        // TODO: Would be nice if we could use the registration info here.
    }

    /**
     * Determine our "creator mod" from a list of upgrades.
     * <p>
     * We attempt to find the first non-vanilla/non-CC upgrade.
     *
     * @param first  The first upgrade.
     * @param second The second upgrade.
     * @return The owning mod id of this item.
     */
    public String getOwner(@Nullable UpgradeData<T> first, @Nullable UpgradeData<T> second) {
        if (first != null) {
            var mod = getOwner(first.holder());
            if (!mod.equals(ComputerCraftAPI.MOD_ID)) return mod;
        }

        if (second != null) {
            var mod = getOwner(second.holder());
            if (!mod.equals(ComputerCraftAPI.MOD_ID)) return mod;
        }

        return ComputerCraftAPI.MOD_ID;
    }

    @Nullable
    public UpgradeData<T> get(HolderLookup.Provider registries, ItemStack stack) {
        return stack.isEmpty() ? null : get(registries.lookupOrThrow(registry), stack);
    }

    @Nullable
    public UpgradeData<T> get(HolderLookup<T> registries, ItemStack stack) {
        if (stack.isEmpty()) return null;

        return registries.listElements()
            .filter(holder -> {
                var upgrade = holder.value();
                var craftingStack = upgrade.getCraftingItem();
                return craftingStack.is(stack.getItem()) && upgrade.isItemSuitable(stack);
            })
            .findAny()
            .map(x -> UpgradeData.of(x, x.value().getUpgradeData(ItemStackTemplate.fromNonEmptyStack(stack))))
            .orElse(null);
    }

    public static Component getName(String baseString, @Nullable UpgradeBase first, @Nullable UpgradeBase second) {
        if (first != null && second != null) {
            return Component.translatable(baseString + ".upgraded_twice", second.getAdjective(), first.getAdjective());
        } else if (first != null) {
            return Component.translatable(baseString + ".upgraded", first.getAdjective());
        } else if (second != null) {
            return Component.translatable(baseString + ".upgraded", second.getAdjective());
        } else {
            return Component.translatable(baseString);
        }
    }
}
