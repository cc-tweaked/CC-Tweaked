// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import static dan200.computercraft.shared.platform.FakePlayerConstants.MAX_REACH;

public final class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer {
    private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
        super(serverLevel, gameProfile);
    }

    static FakePlayer create(ServerLevel serverLevel, GameProfile profile) {
        return new FakePlayer(serverLevel, profile);
    }

    @Override
    public boolean canHarmPlayer(Player other) {
        return true;
    }

    @Override
    public void die(DamageSource damageSource) {
    }

    @Override
    public float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0;
    }

    public double getBlockReach() {
        return MAX_REACH;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return false;
    }
}
