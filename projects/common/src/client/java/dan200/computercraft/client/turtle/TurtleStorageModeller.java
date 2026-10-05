// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import com.mojang.math.Transformation;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import dan200.computercraft.shared.turtle.upgrades.TurtleStorage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * {@link TurtleUpgradeModeller} implementation for {@link TurtleStorage}. This provides a custom tinted model for dyed
 * shulker boxes.
 */
public class TurtleStorageModeller implements TurtleUpgradeModeller<TurtleStorage> {
    private static final ResourceLocation LEFT = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_left");
    private static final ResourceLocation RIGHT = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_right");

    private static final ResourceLocation LEFT_COLOUR = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_left");
    private static final ResourceLocation RIGHT_COLOUR = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_right");

    private static final EnumMap<DyeColor, IntUnaryOperator> SHULKER_COLOURS = Util.make(new EnumMap<>(DyeColor.class), m -> {
        m.put(DyeColor.WHITE, colorGetter(0xFFE6EAEA));
        m.put(DyeColor.ORANGE, colorGetter(0xFFF57410));
        m.put(DyeColor.MAGENTA, colorGetter(0xFFBA3FAF));
        m.put(DyeColor.LIGHT_BLUE, colorGetter(0xFF3BB4DB));
        m.put(DyeColor.YELLOW, colorGetter(0xFFFCC724));
        m.put(DyeColor.LIME, colorGetter(0xFF71BC18));
        m.put(DyeColor.PINK, colorGetter(0xFFF38BAA));
        m.put(DyeColor.GRAY, colorGetter(0xFF3E4246));
        m.put(DyeColor.LIGHT_GRAY, colorGetter(0xFF8C8C83));
        m.put(DyeColor.CYAN, colorGetter(0xFF168792));
        m.put(DyeColor.PURPLE, colorGetter(0xFF7426A9));
        m.put(DyeColor.BLUE, colorGetter(0xFF33359B));
        m.put(DyeColor.BROWN, colorGetter(0xFF734828));
        m.put(DyeColor.GREEN, colorGetter(0xFF546D1C));
        m.put(DyeColor.RED, colorGetter(0xFF9C2522));
        m.put(DyeColor.BLACK, colorGetter(0xFF1F1F23));
    });

    private static IntUnaryOperator colorGetter(int color) {
        return layer -> layer == 0 ? color : -1;
    }

    @Override
    public TransformedModel getModel(TurtleStorage upgrade, @Nullable ITurtleAccess turtle, TurtleSide side) {
        var item = upgrade.getCraftingItem().getItem();
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock) {
            var color = shulkerBoxBlock.getColor();
            if (color == null) {
                return TransformedModel.of(side == TurtleSide.LEFT ? LEFT : RIGHT);
            } else {
                var modelManager = Minecraft.getInstance().getModelManager();
                var model = ClientPlatformHelper.get().getModel(modelManager, side == TurtleSide.LEFT ? LEFT_COLOUR : RIGHT_COLOUR);
                return new TransformedModel(model, Transformation.identity(), SHULKER_COLOURS.get(color));
            }
        } else {
            return TurtleUpgradeModeller.flatItem().getModel(upgrade, turtle, side);
        }
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(LEFT, RIGHT, LEFT_COLOUR, RIGHT_COLOUR);
    }
}
