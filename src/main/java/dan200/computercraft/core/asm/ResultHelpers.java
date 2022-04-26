/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.MethodResult;

final class ResultHelpers
{
    private ResultHelpers()
    {
    }

    static Object[] checkNormalResult( MethodResult result )
    {
        if( result.getCallback() != null )
        {
            // Due to how tasks are implemented, we can't currently return a MethodResult. This is an
            // entirely artificial limitation - we can remove it if it ever becomes an issue.
            throw new IllegalStateException( "Must return MethodResult.of from mainThread function." );
        }

        return result.getResult();
    }
}
