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

public final class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer {
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(0, 0);

    private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
        super(serverLevel, gameProfile);
        refreshDimensions();
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
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return DIMENSIONS;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return false;
    }
}
