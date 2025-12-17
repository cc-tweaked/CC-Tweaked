// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.client;

import com.google.auto.service.AutoService;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.client.turtle.RegisterTurtleModelEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.fml.ModLoader;

import java.util.function.Function;

@AutoService(ComputerCraftAPIClientService.class)
public final class ForgeComputerCraftAPIClientImpl implements ComputerCraftAPIClientService {
    @Override
    public Codec<TurtleUpgradeModel.Unbaked> getTurtleUpgradeModelCodec() {
        var idMapper = new ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends TurtleUpgradeModel.Unbaked>>();
        ModLoader.postEvent(new RegisterTurtleModelEvent(idMapper::put));
        return idMapper.codec(Identifier.CODEC).dispatch(TurtleUpgradeModel.Unbaked::type, Function.identity());
    }
}
