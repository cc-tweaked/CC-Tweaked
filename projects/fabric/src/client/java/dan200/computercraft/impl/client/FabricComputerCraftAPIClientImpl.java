// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.google.auto.service.AutoService;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

@AutoService(ComputerCraftAPIClientService.class)
public final class FabricComputerCraftAPIClientImpl implements FabricComputerCraftAPIClientService {
    private static final ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends TurtleUpgradeModel.Unbaked>> TURTLE_UPGRADE_MODELS = new ExtraCodecs.LateBoundIdMapper<>();
    private static final Codec<TurtleUpgradeModel.Unbaked> TURTLE_UPGRADE_CODEC = TURTLE_UPGRADE_MODELS.codec(ResourceLocation.CODEC).dispatch(TurtleUpgradeModel.Unbaked::type, Function.identity());

    @Override
    public Codec<TurtleUpgradeModel.Unbaked> getTurtleUpgradeModelCodec() {
        return TURTLE_UPGRADE_CODEC;
    }

    @Override
    public void registerTurtleUpgradeModeller(ResourceLocation id, MapCodec<? extends TurtleUpgradeModel.Unbaked> codec) {
        TURTLE_UPGRADE_MODELS.put(id, codec);
    }
}
