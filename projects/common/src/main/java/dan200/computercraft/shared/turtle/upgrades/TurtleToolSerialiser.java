// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class TurtleToolSerialiser implements TurtleUpgradeSerialiser<TurtleTool> {
    public static final TurtleToolSerialiser INSTANCE = new TurtleToolSerialiser();

    private TurtleToolSerialiser() {
    }

    @Override
    public TurtleTool fromJson(ResourceLocation id, JsonObject object) {
        var adjective = GsonHelper.getAsString(object, "adjective", UpgradeBase.getDefaultAdjective(id));
        var toolItem = GsonHelper.getAsItem(object, "item");
        var craftingItem = GsonHelper.getAsItem(object, "craftingItem", toolItem);
        var damageMultiplier = GsonHelper.getAsFloat(object, "damageMultiplier", 3.0f);

        TagKey<Block> breakable = null;
        if (object.has("breakable")) {
            var tag = new ResourceLocation(GsonHelper.getAsString(object, "breakable"));
            breakable = TagKey.create(Registries.BLOCK, tag);
        }

        return new TurtleTool(id, adjective, craftingItem, new ItemStack(toolItem), damageMultiplier, breakable);
    }

    @Override
    public TurtleTool fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        var adjective = buffer.readUtf();
        var craftingItem = RegistryWrappers.readId(buffer, RegistryWrappers.ITEMS);
        var toolItem = buffer.readItem();
        // damageMultiplier and breakable aren't used by the client, but we need to construct the upgrade exactly
        // as otherwise syncing on an SP world will overwrite the (shared) upgrade registry with an invalid upgrade!
        var damageMultiplier = buffer.readFloat();

        var breakable = buffer.readBoolean() ? TagKey.create(Registries.BLOCK, buffer.readResourceLocation()) : null;
        return new TurtleTool(id, adjective, craftingItem, toolItem, damageMultiplier, breakable);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, TurtleTool upgrade) {
        buffer.writeUtf(upgrade.getUnlocalisedAdjective());
        RegistryWrappers.writeId(buffer, RegistryWrappers.ITEMS, upgrade.getCraftingItem().getItem());
        buffer.writeItem(upgrade.item);
        buffer.writeFloat(upgrade.damageMulitiplier);
        buffer.writeBoolean(upgrade.breakable != null);
        if (upgrade.breakable != null) buffer.writeResourceLocation(upgrade.breakable.location());
    }
}
