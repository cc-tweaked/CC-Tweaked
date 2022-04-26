/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ObjectLuaTable implements LuaTable<Object, Object>
{
    private final Map<Object, Object> map;

    public ObjectLuaTable( Map<?, ?> map )
    {
        this.map = Collections.unmodifiableMap( map );
    }

    @Override
    public int size()
    {
        return map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey( Object o )
    {
        return map.containsKey( o );
    }

    @Override
    public boolean containsValue( Object o )
    {
        return map.containsKey( o );
    }

    @Override
    public Object get( Object o )
    {
        return map.get( o );
    }

    @Nonnull
    @Override
    public Set<Object> keySet()
    {
        return map.keySet();
    }

    @Nonnull
    @Override
    public Collection<Object> values()
    {
        return map.values();
    }

    @Nonnull
    @Override
    public Set<Entry<Object, Object>> entrySet()
    {
        return map.entrySet();
    }
}
