// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.model.turtle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Combines several individual models together to form a turtle.
 */
public final class TurtleModelParts {
    private static final Transformation identity, flip;

    static {
        var stack = new PoseStack();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.scale(1, -1, 1);
        stack.translate(-0.5f, -0.5f, -0.5f);

        identity = Transformation.identity();
        flip = new Transformation(stack.last().pose());
    }

    public record Combination(
        boolean colour,
        @Nullable ITurtleUpgrade leftUpgrade,
        @Nullable ITurtleUpgrade rightUpgrade,
        @Nullable ResourceLocation overlay,
        boolean christmas,
        boolean flip
    ) {
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;
    private final Function<TransformedModel, BakedModel> transformer;

    /**
     * A cache of {@link TransformedModel} to the transformed {@link BakedModel}. This helps us pool the transformed
     * instances, reducing memory usage and hopefully ensuring their caches are hit more often!
     */
    private final Map<TransformedModel, BakedModel> transformCache = new HashMap<>();

    public TurtleModelParts(BakedModel familyModel, BakedModel colourModel, ModelTransformer transformer) {
        this.familyModel = familyModel;
        this.colourModel = colourModel;
        this.transformer = x -> transformer.transform(x.getModel(), x.getMatrix());
    }

    public Combination getCombination(ItemStack stack) {
        var christmas = Holiday.getCurrent() == Holiday.CHRISTMAS;

        if (!(stack.getItem() instanceof TurtleItem turtle)) {
            return new Combination(false, null, null, null, christmas, false);
        }

        var colour = turtle.getColour(stack);
        var leftUpgrade = turtle.getUpgrade(stack, TurtleSide.LEFT);
        var rightUpgrade = turtle.getUpgrade(stack, TurtleSide.RIGHT);
        var overlay = turtle.getOverlay(stack);
        var label = turtle.getLabel(stack);
        var flip = label != null && (label.equals("Dinnerbone") || label.equals("Grumm"));

        return new Combination(colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip);
    }

    public List<BakedModel> buildModel(Combination combo) {
        var mc = Minecraft.getInstance();
        var modelManager = mc.getItemRenderer().getItemModelShaper().getModelManager();

        var transformation = combo.flip ? flip : identity;
        var parts = new ArrayList<BakedModel>(4);
        parts.add(transform(combo.colour() ? colourModel : familyModel, transformation));

        var overlayModelLocation = TurtleBlockEntityRenderer.getTurtleOverlayModel(combo.overlay(), combo.christmas());
        if (overlayModelLocation != null) {
            parts.add(transform(ClientPlatformHelper.get().getModel(modelManager, overlayModelLocation), transformation));
        }
        if (combo.leftUpgrade() != null) {
            var model = TurtleUpgradeModellers.getModel(combo.leftUpgrade(), null, TurtleSide.LEFT);
            parts.add(transform(model.getModel(), transformation.compose(model.getMatrix())));
        }
        if (combo.rightUpgrade() != null) {
            var model = TurtleUpgradeModellers.getModel(combo.rightUpgrade(), null, TurtleSide.RIGHT);
            parts.add(transform(model.getModel(), transformation.compose(model.getMatrix())));
        }

        return parts;
    }

    public BakedModel transform(BakedModel model, Transformation transformation) {
        if (transformation.equals(Transformation.identity())) return model;
        return transformCache.computeIfAbsent(new TransformedModel(model, transformation), transformer);
    }

    public interface ModelTransformer {
        BakedModel transform(BakedModel model, Transformation transformation);
    }
}
