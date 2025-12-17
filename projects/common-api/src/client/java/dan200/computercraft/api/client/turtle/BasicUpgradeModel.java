// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.resources.Identifier;

/**
 * A {@link TurtleUpgradeModel} that renders a basic model.
 * <p>
 * This is the {@link TurtleUpgradeModel} equivalent of {@link BlockModelWrapper}.
 */
public final class BasicUpgradeModel implements TurtleUpgradeModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "sided");
    public static final MapCodec<? extends TurtleUpgradeModel.Unbaked> CODEC = RecordCodecBuilder.<Unbaked>mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("left").forGetter(Unbaked::left),
        Identifier.CODEC.fieldOf("right").forGetter(Unbaked::right)
    ).apply(instance, Unbaked::new));

    private final StandaloneModel left;
    private final StandaloneModel right;

    private BasicUpgradeModel(StandaloneModel left, StandaloneModel right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Create an unbaked {@link BasicUpgradeModel}.
     *
     * @param left  The model when equipped on the left.
     * @param right The model when equipped on the right.
     * @return The unbaked turtle upgrade model.
     */
    public static TurtleUpgradeModel.Unbaked unbaked(Identifier left, Identifier right) {
        return new Unbaked(left, right);
    }

    private StandaloneModel getModel(TurtleSide side) {
        return switch (side) {
            case LEFT -> left;
            case RIGHT -> right;
        };
    }

    @Override
    public void renderForItem(UpgradeData<ITurtleUpgrade> upgrade, TurtleSide side, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed) {
        renderer.appendModelIdentityElement(this);
        renderer.appendModelIdentityElement(side);
        renderer.appendModelIdentityElement(transform);

        var layer = renderer.newLayer();
        layer.setTransform(transform);
        getModel(side).setupItemLayer(layer);
    }

    private record Unbaked(Identifier left, Identifier right) implements TurtleUpgradeModel.Unbaked {
        @Override
        public MapCodec<? extends TurtleUpgradeModel.Unbaked> type() {
            return CODEC;
        }

        @Override
        public TurtleUpgradeModel bake(ModelBaker baker) {
            return new BasicUpgradeModel(StandaloneModel.of(left(), baker), StandaloneModel.of(right(), baker));
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(left());
            resolver.markDependency(right());
        }
    }
}
