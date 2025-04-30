// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;

record SidedUpgradeModel<T extends ITurtleUpgrade>(
    StandaloneModel left, StandaloneModel right
) implements TurtleUpgradeModelViaStandalone<T> {
    @Override
    public StandaloneModel getModel(T upgrade, TurtleSide side, DataComponentPatch data) {
        return switch (side) {
            case LEFT -> left();
            case RIGHT -> right();
        };
    }

    record Unbaked<T extends ITurtleUpgrade>(
        ResourceLocation left, ResourceLocation right
    ) implements TurtleUpgradeModel.Unbaked<T> {

        @Override
        public TurtleUpgradeModel<T> bake(ModelBaker baker) {
            return new SidedUpgradeModel<>(StandaloneModel.of(left(), baker), StandaloneModel.of(right(), baker));
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(left());
            resolver.markDependency(right());
        }
    }
}
