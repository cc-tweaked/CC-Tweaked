// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ForgeModRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;

/**
 * A {@link LootModifier} which adds a new loot pool to a piece of loot.
 */
public final class InjectLootTableModifier extends LootModifier {
    private final ResourceLocation location;

    public InjectLootTableModifier(LootItemCondition[] conditionsIn, ResourceLocation location) {
        super(conditionsIn);
        this.location = location;
    }

    public static Codec<InjectLootTableModifier> createCodec() {
        return RecordCodecBuilder.create(inst -> LootModifier.codecStart(inst).and(
                ResourceLocation.CODEC.fieldOf("loot_table").forGetter(m -> m.location)
            ).apply(inst, InjectLootTableModifier::new)
        );
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        context.getResolver().getLootTable(location).getRandomItemsRaw(context, generatedLoot::add);
        return generatedLoot;
    }

    @Override
    public Codec<InjectLootTableModifier> codec() {
        return ForgeModRegistry.Codecs.INJECT_LOOT_TABLE.get();
    }
}
