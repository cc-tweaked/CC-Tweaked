/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.gametest;

import dan200.computercraft.gametest.core.TestHooks;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameTestSequence.class)
class GameTestSequenceMixin {
    @Shadow
    @Final
    GameTestInfo parent;

    /**
     * Override {@link GameTestSequence#tickAndContinue(long)} to catch non-{@link GameTestAssertException} failures.
     *
     * @param ticks The current tick.
     * @author Jonathan Coates
     * @reason There's no sense doing this in a more compatible way for game tests.
     */
    @Overwrite
    public void tickAndContinue(long ticks) {
        try {
            tick(ticks);
        } catch (GameTestAssertException ignored) {
            // Mimic the original behaviour.
        } catch (AssertionError e) {
            parent.fail(e);
        } catch (Exception e) {
            // Fail the test, rather than crashing the server.
            TestHooks.LOG.error("{} threw unexpected exception", parent.getTestName(), e);
            parent.fail(e);
        }
    }

    @Shadow
    private void tick(long tick) {
    }
}
