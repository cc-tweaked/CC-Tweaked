/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A test which manipulates the game. This should applied on an instance function, and should accept a single
 * {@link GameTestHelper} argument.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface GameTest
{
    /**
     * Maximum time the test can run, in ticks.
     *
     * @return The time the test can run in ticks.
     */
    int timeoutTicks() default 200;

    /**
     * Number of ticks to delay between building the structure and running the test code.
     *
     * @return Test delay in ticks.
     */
    long setup() default 5;

    /**
     * The batch to run tests in. This may be used to run tests which manipulate other bits of state.
     *
     * @return This test's batch.
     */
    String batch() default "default";

    /**
     * If this test must pass. When false, test failures do not cause a build failure.
     *
     * @return If this test is required.
     */
    boolean required() default true;
}
