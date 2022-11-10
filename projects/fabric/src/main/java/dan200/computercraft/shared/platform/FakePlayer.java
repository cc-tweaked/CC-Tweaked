/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.OptionalInt;

final class FakePlayer extends ServerPlayer {
    private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
        super(serverLevel.getServer(), serverLevel, gameProfile, null);
        connection = new FakeNetHandler(this);
    }

    static FakePlayer create(ServerLevel serverLevel, GameProfile profile) {
        // Restore the previous player's advancements. See #564.
        var playerList = serverLevel.getServer().getPlayerList();
        var currentPlayer = playerList.getPlayer(profile.getId());

        var fakePlayer = new FakePlayer(serverLevel, profile);
        if (currentPlayer != null) fakePlayer.getAdvancements().setPlayer(currentPlayer);

        return fakePlayer;
    }

    @Override
    public void tick() {
    }

    @Override
    public void doTick() {
        super.doTick();
    }

    @Override
    public void awardStat(Stat<?> stat, int increment) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canHarmPlayer(Player other) {
        return true;
    }

    @Override
    public void die(DamageSource damageSource) {
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
}
