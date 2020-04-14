/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ILuaAPI;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dan200.computercraft.api.lua.ArgumentHelper.*;

public class JSONAPI implements ILuaAPI {

    private void getJsonElement(JsonElement element, Object name, Map<Object, Object> table) {
        if (element.isJsonPrimitive()) {
            if ( element.getAsJsonPrimitive().isBoolean() )
                table.put( name, element.getAsBoolean() );
            if ( element.getAsJsonPrimitive().isString() )
                table.put( name, element.getAsJsonPrimitive().getAsString() );
            if ( element.getAsJsonPrimitive().isNumber() ){
                table.put( name, element.getAsNumber() );
            }
        }
        if (element.isJsonArray()) {
            int count = 0;
            Map<Object, Object> array = new HashMap<>();
            for (JsonElement pa : element.getAsJsonArray()) {
                getJsonElement(pa, count, array);
                count++;
            }
            table.put( name, array);
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();
            Map<Object, Object> list = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry: entries) {
                JsonElement v = entry.getValue();
                String key = entry.getKey();
                if ( name.equals("") ) {
                    getJsonElement(v, key, table);
                }
                else {
                    getJsonElement(v, key, list);
                }
            }
            if ( !name.equals("") ) {
                table.put( name, list );
            }
        }
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "json" };
    }

     @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "load",
        };
    }

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException {
        switch( method ) {
            case 0: {
                // load
                String text = getString(args, 0);
                Map<Object, Object> table = new HashMap<>();
                JsonParser parser = new JsonParser();
                JsonElement element = parser.parse(text);
                getJsonElement(element, "", table);
                return new Object[]{table};
            }
            default: {
                return null;
            }
        }
    }

}
