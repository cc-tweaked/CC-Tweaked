// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.item.properties;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.turtle.TurtleOverlay;
import dan200.computercraft.client.turtle.TurtleOverlayManager;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An item property that determines whether the turtle's current {@linkplain TurtleOverlay overlay} is compatible
 * with the Christmas overlay.
 *
 * @see TurtleOverlay#showElfOverlay()
 */
public class TurtleShowElfOverlay implements ConditionalItemModelProperty {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "turtle/show_elf_overlay");
    private static final TurtleShowElfOverlay INSTANCE = new TurtleShowElfOverlay();
    public static final MapCodec<TurtleShowElfOverlay> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int i, ItemDisplayContext context) {
        var overlay = TurtleOverlayManager.get(Minecraft.getInstance().getModelManager(), TurtleItem.getOverlay(stack));
        return overlay == null || overlay.showElfOverlay();
    }

    public static TurtleShowElfOverlay create() {
        return INSTANCE;
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return CODEC;
    }
}
