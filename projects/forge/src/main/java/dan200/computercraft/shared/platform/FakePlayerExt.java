// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.OptionalInt;

class FakePlayerExt extends FakePlayer {
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(0, 0);

    FakePlayerExt(ServerLevel serverLevel, GameProfile profile) {
        super(serverLevel, profile);
    }

    @Override
    public void doTick() {
        super.doTick();
    }

    @Override
    public boolean canHarmPlayer(Player other) {
        return true;
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider menu) {
        return OptionalInt.empty();
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return DIMENSIONS;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return false;
    }
}
