/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import java.util.Collection;

public class LuaUtil
{
    public static Object[] consArray( Object value, Collection<?> rest )
    {
        if( rest.isEmpty() ) return new Object[] { value };

        // I'm not proud of this code.
        Object[] out = new Object[rest.size() + 1];
        out[0] = value;
        int i = 1;
        for( Object additionalType : rest ) out[i++] = additionalType;
        return out;
    }
}
