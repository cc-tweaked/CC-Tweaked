// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.turtle.TurtleUpgradeModelManager;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ItemModel} that renders a turtle upgrade, using its {@link dan200.computercraft.api.client.turtle.TurtleUpgradeModel}.
 *
 * @param side The side the upgrade resides on.
 * @param base The base model. Only used to provide item transforms.
 */
public record TurtleUpgradeModel(TurtleSide side, ItemTransforms base) implements ItemModel {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "turtle/upgrade");
    public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        TurtleSide.CODEC.fieldOf("side").forGetter(Unbaked::side),
        ResourceLocation.CODEC.fieldOf("transforms").forGetter(Unbaked::base)
    ).apply(instance, Unbaked::new));

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable ItemOwner holder, int seed) {
        var upgrade = TurtleItem.getUpgradeWithData(stack, side);
        if (upgrade == null) return;

        TurtleUpgradeModelManager.get(Minecraft.getInstance().getModelManager(), upgrade.holder())
            .renderForItem(upgrade, side, state, resolver, base.getTransform(context), seed);
    }

    public record Unbaked(TurtleSide side, ResourceLocation base) implements ItemModel.Unbaked {
        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public ItemModel bake(BakingContext bakingContext) {
            return new TurtleUpgradeModel(side, bakingContext.blockModelBaker().getModel(base).getTopTransforms());
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(base);
        }
    }
}
