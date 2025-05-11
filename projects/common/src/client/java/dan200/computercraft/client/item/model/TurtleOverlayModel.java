// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.turtle.TurtleOverlay;
import dan200.computercraft.client.turtle.TurtleOverlayManager;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ItemModel} that renders the {@linkplain TurtleOverlay turtle overlay}.
 *
 * @param transforms The item transformations from the base model.
 * @see TurtleOverlay#model()
 */
public record TurtleOverlayModel(ItemTransforms transforms) implements ItemModel {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "turtle/overlay");
    public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("transforms").forGetter(Unbaked::base)
    ).apply(instance, Unbaked::new));

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable LivingEntity holder, int light) {
        var overlay = TurtleItem.getOverlay(stack);
        if (overlay == null) return;

        var layer = state.newLayer();
        TurtleOverlayManager.get(Minecraft.getInstance().getModelManager(), overlay).model().setupItemLayer(layer);
        layer.setTransform(transforms().getTransform(context));
    }

    public record Unbaked(ResourceLocation base) implements ItemModel.Unbaked {
        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public ItemModel bake(BakingContext bakingContext) {
            return new TurtleOverlayModel(bakingContext.blockModelBaker().getModel(base).getTopTransforms());
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
        }
    }
}
