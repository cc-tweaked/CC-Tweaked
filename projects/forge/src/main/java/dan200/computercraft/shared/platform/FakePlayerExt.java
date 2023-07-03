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
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.OptionalInt;

import static dan200.computercraft.shared.platform.FakePlayerConstants.MAX_REACH;

class FakePlayerExt extends FakePlayer {
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
    public float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0;
    }

    @Override
    public double getBlockReach() {
        return MAX_REACH;
    }

    @Override
    public double getEntityReach() {
        return MAX_REACH;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return false;
    }
}
