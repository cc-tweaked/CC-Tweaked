// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import dan200.computercraft.shared.turtle.upgrades.TurtleStorage;

/**
 * {@link TurtleUpgradeModeller} implementation for {@link TurtleStorage}. This provides a custom tinted model for dyed
 * shulker boxes.
 */
public final class TurtleStorageModeller {
    /*private static final ResourceLocation LEFT = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_left");
    private static final ResourceLocation RIGHT = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_right");

    private static final ResourceLocation LEFT_COLOUR = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_left");
    private static final ResourceLocation RIGHT_COLOUR = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_shulker_colour_right");

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
    });*/
/*
    private static IntUnaryOperator colorGetter(int color) {
        return layer -> layer == 0 ? color : -1;
    }

    @Override
    public TransformedModel getModel(TurtleStorage upgrade, @Nullable ITurtleAccess turtle, TurtleSide side, DataComponentPatch data) {
        if (turtle != null) {
            var handle = turtle.getUpgradeHandle(side);
            if (handle != null) {
                var model = getImpl(handle.getItem(), side);
                if (model != null) return model;
            }
        }

        return TurtleUpgradeModeller.flatItem().getModel(upgrade, turtle, side, data);
    }

    @Override
    public TransformedModel getModel(TurtleStorage upgrade, UpgradeData<ITurtleUpgrade> data, TurtleSide side) {
        var model = getImpl(data.getUpgradeItem(), side);
        return model == null ? TurtleUpgradeModeller.flatItem().getModel(upgrade, data, side) : model;
    }

    private static @Nullable TransformedModel getImpl(ItemStack item, TurtleSide side) {
        if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock) {
            var color = shulkerBoxBlock.getColor();
            if (color == null) {
                return TransformedModel.of(side == TurtleSide.LEFT ? LEFT : RIGHT);
            } else {
                var modelManager = Minecraft.getInstance().getModelManager();
                var model = ClientPlatformHelper.get().getModel(modelManager, side == TurtleSide.LEFT ? LEFT_COLOUR : RIGHT_COLOUR);
                return new TransformedModel(model, Transformation.identity(), SHULKER_COLOURS.get(color));
            }
        } else {
            return null;
        }
    }

    @Override
    public Stream<ResourceLocation> getDependencies() {
        return Stream.of(LEFT, RIGHT, LEFT_COLOUR, RIGHT_COLOUR);
    }*/
}
