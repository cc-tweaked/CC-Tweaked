// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.EnumMap;

/**
 * {@link TurtleUpgradeModel} implementation for shulker boxes. This provides a custom tinted model for dyed shulker
 * boxes.
 */
public final class TurtleShulkerModel implements TurtleUpgradeModel {
    private static final Unbaked INSTANCE = new Unbaked();

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "shulker");
    public static final MapCodec<TurtleUpgradeModel.Unbaked> CODEC = MapCodec.unit(INSTANCE);

    private static final Identifier LEFT = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_left");
    private static final Identifier RIGHT = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_right");

    private static final Identifier LEFT_COLOUR = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_left");
    private static final Identifier RIGHT_COLOUR = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_right");

    private static final EnumMap<DyeColor, Integer> SHULKER_COLOURS = Util.make(new EnumMap<>(DyeColor.class), m -> {
        m.put(DyeColor.WHITE, 0xFFE6EAEA);
        m.put(DyeColor.ORANGE, 0xFFF57410);
        m.put(DyeColor.MAGENTA, 0xFFBA3FAF);
        m.put(DyeColor.LIGHT_BLUE, 0xFF3BB4DB);
        m.put(DyeColor.YELLOW, 0xFFFCC724);
        m.put(DyeColor.LIME, 0xFF71BC18);
        m.put(DyeColor.PINK, 0xFFF38BAA);
        m.put(DyeColor.GRAY, 0xFF3E4246);
        m.put(DyeColor.LIGHT_GRAY, 0xFF8C8C83);
        m.put(DyeColor.CYAN, 0xFF168792);
        m.put(DyeColor.PURPLE, 0xFF7426A9);
        m.put(DyeColor.BLUE, 0xFF33359B);
        m.put(DyeColor.BROWN, 0xFF734828);
        m.put(DyeColor.GREEN, 0xFF546D1C);
        m.put(DyeColor.RED, 0xFF9C2522);
        m.put(DyeColor.BLACK, 0xFF1F1F23);
    });

    private TurtleShulkerModel(StandaloneModel left, StandaloneModel right, StandaloneModel leftColour, StandaloneModel rightColour) {
        this.left = left;
        this.right = right;
        this.leftColour = leftColour;
        this.rightColour = rightColour;
    }

    private final StandaloneModel left, right, leftColour, rightColour;

    @Override
    public void renderForItem(UpgradeHandle<ITurtleUpgrade> upgrade, TurtleSide side, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed) {
        var colour = upgrade.getItem().item().value() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock block
            ? block.getColor()
            : null;

        renderer.appendModelIdentityElement(this);
        renderer.appendModelIdentityElement(side);
        if (colour != null) renderer.appendModelIdentityElement(colour);

        var layer = renderer.newLayer();
        layer.setItemTransform(transform);
        if (colour == null) {
            (side == TurtleSide.LEFT ? left : right).setupItemLayer(layer);
        } else {
            (side == TurtleSide.LEFT ? leftColour : rightColour).setupItemLayer(layer);
            layer.tintLayers().add((int) SHULKER_COLOURS.getOrDefault(colour, -1));
        }
    }

    /**
     * Get the unbaked {@link TurtleShulkerModel}.
     *
     * @return The unbaked shulker upgrade model.
     */
    public static TurtleUpgradeModel.Unbaked unbaked() {
        return INSTANCE;
    }

    private static final class Unbaked implements TurtleUpgradeModel.Unbaked {
        @Override
        public MapCodec<? extends TurtleUpgradeModel.Unbaked> type() {
            return CODEC;
        }

        @Override
        public TurtleUpgradeModel bake(ModelBaker baker) {
            return new TurtleShulkerModel(
                StandaloneModel.of(LEFT, baker),
                StandaloneModel.of(RIGHT, baker),
                StandaloneModel.of(LEFT_COLOUR, baker),
                StandaloneModel.of(RIGHT_COLOUR, baker)
            );
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(LEFT);
            resolver.markDependency(RIGHT);
            resolver.markDependency(LEFT_COLOUR);
            resolver.markDependency(RIGHT_COLOUR);
        }
    }
}
