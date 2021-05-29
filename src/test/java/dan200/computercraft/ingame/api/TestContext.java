/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.api;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.TestTracker;
import net.minecraft.test.TestTrackerHolder;
import net.minecraft.test.TestUtils;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

/**
 * The context a test is run within.
 *
 * @see TestExtensionsKt For additional test helper methods.
 */
public final class TestContext
{
    private final TestTracker tracker;

    public TestContext( TestTrackerHolder holder )
    {
        this.tracker = ObfuscationReflectionHelper.getPrivateValue( TestTrackerHolder.class, holder, "field_229487_a_" );
    }

    public TestTracker getTracker()
    {
        return tracker;
    }

    public void ok()
    {
        try
        {
            Method finish = TestTracker.class.getDeclaredMethod( "finish" );
            finish.setAccessible( true );
            finish.invoke( tracker );

            Method spawn = TestUtils.class.getDeclaredMethod( "spawnBeacon", TestTracker.class, Block.class );
            spawn.setAccessible( true );
            spawn.invoke( null, tracker, Blocks.LIME_STAINED_GLASS );
        }
        catch( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void fail( Throwable e )
    {
        if( !tracker.isDone() ) tracker.fail( e );
    }

    public boolean isDone()
    {
        return tracker.isDone();
    }
}
