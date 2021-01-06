/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle.event;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleVerb;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Fired when a turtle attempts to attack an entity.
 *
 * This must be fired by {@link ITurtleUpgrade#useTool(ITurtleAccess, TurtleSide, TurtleVerb, Direction)},
 * as the base {@code turtle.attack()} command does not fire it.
 *
 * Note that such commands should also fire {@link AttackEntityEvent}, so you do not need to listen to both.
 *
 * @see TurtleAction#ATTACK
 */
public class TurtleAttackEvent extends TurtlePlayerEvent
{
    private final Entity target;
    private final ITurtleUpgrade upgrade;
    private final TurtleSide side;

    public TurtleAttackEvent( @Nonnull ITurtleAccess turtle, @Nonnull FakePlayer player, @Nonnull Entity target, @Nonnull ITurtleUpgrade upgrade, @Nonnull TurtleSide side )
    {
        super( turtle, TurtleAction.ATTACK, player );
        Objects.requireNonNull( target, "target cannot be null" );
        Objects.requireNonNull( upgrade, "upgrade cannot be null" );
        Objects.requireNonNull( side, "side cannot be null" );
        this.target = target;
        this.upgrade = upgrade;
        this.side = side;
    }

    /**
     * Get the entity being attacked by this turtle.
     *
     * @return The entity being attacked.
     */
    @Nonnull
    public Entity getTarget()
    {
        return target;
    }

    /**
     * Get the upgrade responsible for attacking.
     *
     * @return The upgrade responsible for attacking.
     */
    @Nonnull
    public ITurtleUpgrade getUpgrade()
    {
        return upgrade;
    }

    /**
     * Get the side the attacking upgrade is on.
     *
     * @return The upgrade's side.
     */
    @Nonnull
    public TurtleSide getSide()
    {
        return side;
    }
}
