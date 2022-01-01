/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.BiFunction;
import java.util.function.Consumer;

class BasicCustomLoader<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T>
{
    private final Consumer<JsonObject> extra;

    protected BasicCustomLoader( ResourceLocation loaderId, T parent, ExistingFileHelper existingFileHelper, Consumer<JsonObject> extra )
    {
        super( loaderId, parent, existingFileHelper );
        this.extra = extra;
    }

    public static <T extends ModelBuilder<T>> BiFunction<T, ExistingFileHelper, CustomLoaderBuilder<T>> makeFactory( ResourceLocation id, Consumer<JsonObject> extra )
    {
        return ( parent, x ) -> new BasicCustomLoader<>( id, parent, x, extra );
    }

    @Override
    public JsonObject toJson( JsonObject json )
    {
        super.toJson( json );
        extra.accept( json );
        return json;
    }
}
