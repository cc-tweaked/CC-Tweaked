// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.turtle.TurtleToolDurability;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingIngredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public final class TurtleToolSerialiser implements TurtleUpgradeSerialiser<TurtleTool> {
    public static final TurtleToolSerialiser INSTANCE = new TurtleToolSerialiser();

    private TurtleToolSerialiser() {
    }

    @Override
    public TurtleTool fromJson(ResourceLocation id, JsonObject object) {
        var adjective = GsonHelper.getAsString(object, "adjective", UpgradeBase.getDefaultAdjective(id));

        var item = SerialiserWithCraftingIngredient.getIngredientOrItem(object, "item");
        var damageMultiplier = GsonHelper.getAsFloat(object, "damageMultiplier", 3.0f);
        var allowEnchantments = GsonHelper.getAsBoolean(object, "allowEnchantments", false);
        var consumeDurability = TurtleToolDurability.CODEC.byName(GsonHelper.getAsString(object, "consumeDurability", null), TurtleToolDurability.NEVER);

        TagKey<Block> breakable = null;
        if (object.has("breakable")) {
            var tag = new ResourceLocation(GsonHelper.getAsString(object, "breakable"));
            breakable = TagKey.create(Registries.BLOCK, tag);
        }

        return new TurtleTool(id, adjective, item, damageMultiplier, allowEnchantments, consumeDurability, breakable);
    }

    @Override
    public TurtleTool fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        var adjective = buffer.readUtf();
        var item = Ingredient.fromNetwork(buffer);
        // damageMultiplier and breakable aren't used by the client, but we need to construct the upgrade exactly
        // as otherwise syncing on an SP world will overwrite the (shared) upgrade registry with an invalid upgrade!
        var damageMultiplier = buffer.readFloat();
        var allowsEnchantments = buffer.readBoolean();
        var consumesDurability = buffer.readEnum(TurtleToolDurability.class);

        var breakable = buffer.readNullable(b -> TagKey.create(Registries.BLOCK, b.readResourceLocation()));
        return new TurtleTool(id, adjective, item, damageMultiplier, allowsEnchantments, consumesDurability, breakable);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, TurtleTool upgrade) {
        buffer.writeUtf(upgrade.getUnlocalisedAdjective());
        upgrade.getCraftingIngredient().toNetwork(buffer);
        buffer.writeFloat(upgrade.damageMultiplier);
        buffer.writeBoolean(upgrade.allowEnchantments);
        buffer.writeEnum(upgrade.consumeDurability);
        buffer.writeNullable(upgrade.breakable, (b, x) -> b.writeResourceLocation(x.location()));
    }
}
