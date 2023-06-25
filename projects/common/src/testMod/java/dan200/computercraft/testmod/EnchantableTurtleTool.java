package dan200.computercraft.testmod;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.upgrades.TurtleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EnchantableTurtleTool extends TurtleTool {
    private static final String ENCHANTMENTS_TAG = "Enchantments";
    public EnchantableTurtleTool(ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem, float damageMulitiplier, @Nullable TagKey<Block> breakable) {
        super(id, adjective, craftItem, toolItem, damageMulitiplier, breakable);
    }

    @Override
    protected ItemStack buildItem(ITurtleAccess turtle, TurtleSide side) {
        var upgradeData = turtle.getUpgradeData(side);
        if (upgradeData == null) return super.buildItem(turtle, side);
        return upgradeData.getUpgradeItem();
    }

    @Override
    public boolean isItemSuitable(ItemStack stack) {
        // So, we want to make item enchant to be possible, right?
        // Let's start with tweaking enchanting logic
        if (stack.isEnchanted()) {
            var disenchantedStack = stack.copy();
            var tag = disenchantedStack.getOrCreateTag();
            tag.remove(ENCHANTMENTS_TAG);
            disenchantedStack.setTag(tag);
            // The rest of logic is pretty fine for our case
            return super.isItemSuitable(disenchantedStack);
        }
        return super.isItemSuitable(stack);
    }

    @NotNull
    @Override
    public CompoundTag getUpgradeData(@NotNull ItemStack stack) {
        var newTag = new CompoundTag();
        var itemTag = stack.getOrCreateTag();
        if (itemTag.contains(ENCHANTMENTS_TAG)) newTag.put(ENCHANTMENTS_TAG, Objects.requireNonNull(itemTag.get(ENCHANTMENTS_TAG)));
        return newTag;
    }

    @NotNull
    @Override
    public ItemStack getUpgradeItem(@NotNull CompoundTag upgradeData) {
        var baseItem = super.getUpgradeItem(upgradeData);
        if (upgradeData.contains(ENCHANTMENTS_TAG)) {
            baseItem.getOrCreateTag().put(ENCHANTMENTS_TAG, Objects.requireNonNull(upgradeData.get(ENCHANTMENTS_TAG)));
        }
        return baseItem;
    }
}
