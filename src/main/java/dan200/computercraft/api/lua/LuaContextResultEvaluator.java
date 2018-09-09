/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Evaluates {@link MethodResult}s within a {@link ILuaContext}.
 *
 * @see MethodResult#evaluate(ILuaContext)
 * @see MethodResult#withLuaContext(ILuaContextTask)
 * @deprecated This should not be used except to interface between the two call call systems.
 */
@Deprecated
class LuaContextResultEvaluator
{
    @Deprecated
    public static Object[] evaluate( @Nonnull ILuaContext context, @Nonnull MethodResult future ) throws LuaException, InterruptedException
    {
        Deque<ILuaFunction> callbacks = null;
        while( true )
        {
            if( future instanceof MethodResult.AndThen )
            {
                MethodResult.AndThen then = ((MethodResult.AndThen) future);

                // Thens are "unwrapped", being pushed onto a stack
                if( callbacks == null ) callbacks = new ArrayDeque<>();
                callbacks.addLast( then.getCallback() );

                future = then.getPrevious();
                if( future == null ) throw new NullPointerException( "Null result from " + then.getCallback() );
            }
            else if( future instanceof MethodResult.Immediate )
            {
                Object[] values = ((MethodResult.Immediate) future).getResult();

                // Immediate values values will attempt to call the previous "then", or return if nothing 
                // else needs to be done.
                ILuaFunction callback = callbacks == null ? null : callbacks.pollLast();
                if( callback == null ) return values;

                future = callback.call( values );
                if( future == null ) throw new NullPointerException( "Null result from " + callback );
            }
            else if( future instanceof MethodResult.OnEvent )
            {
                MethodResult.OnEvent onEvent = (MethodResult.OnEvent) future;

                // Poll for an event, and then call the previous "then" or return if nothing else needs 
                // to be done. 
                Object[] values = onEvent.isRaw() ? context.pullEventRaw( onEvent.getFilter() ) : context.pullEvent( onEvent.getFilter() );

                ILuaFunction callback = callbacks == null ? null : callbacks.pollLast();
                if( callback == null ) return values;

                future = callback.call( values );
                if( future == null ) throw new NullPointerException( "Null result from " + callback );
            }
            else if( future instanceof MethodResult.OnMainThread )
            {
                MethodResult.OnMainThread onMainThread = (MethodResult.OnMainThread) future;

                // Evaluate our task on the main thread and mark it as the next future to evaluate.
                Reference temporary = new Reference();
                context.executeMainThreadTask( () -> {
                    temporary.value = onMainThread.getTask().execute();
                    return null;
                } );

                future = temporary.value;
                if( future == null ) throw new NullPointerException( "Null result from " + onMainThread.getTask() );
            }
            else if( future instanceof MethodResult.WithLuaContext )
            {
                MethodResult.WithLuaContext withContext = (MethodResult.WithLuaContext) future;

                // Run the task, and then call the previous "then" or return if nothing else
                // needs to be done. 
                Object[] values = withContext.getConsumer().execute( context );

                ILuaFunction callback = callbacks == null ? null : callbacks.pollLast();
                if( callback == null ) return values;
                
                future = callback.call( values );
                if( future == null ) throw new NullPointerException( "Null result from " + callback );
            }
            else
            {
                throw new IllegalStateException( "Unknown MethodResult " + future );
            }
        }
    }

    private static class Reference
    {
        MethodResult value;
    }
}
