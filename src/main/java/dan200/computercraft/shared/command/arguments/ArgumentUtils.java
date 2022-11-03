/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

/**
 * Utilities for working with arguments.
 *
 * @see net.minecraft.commands.synchronization.ArgumentUtils
 */
public class ArgumentUtils {
    public static <A extends ArgumentType<?>> JsonObject serializeToJson(ArgumentTypeInfo.Template<A> template) {
        var object = new JsonObject();
        object.addProperty("type", "argument");
        object.addProperty("parser", ForgeRegistries.COMMAND_ARGUMENT_TYPES.getKey(template.type()).toString());

        var properties = new JsonObject();
        serializeToJson(properties, template.type(), template);
        if (properties.size() > 0) object.add("properties", properties);

        return object;
    }

    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeToJson(JsonObject jsonObject, ArgumentTypeInfo<A, T> argumentTypeInfo, ArgumentTypeInfo.Template<A> template) {
        argumentTypeInfo.serializeToJson((T) template, jsonObject);
    }

    public static <A extends ArgumentType<?>> void serializeToNetwork(FriendlyByteBuf buffer, ArgumentTypeInfo.Template<A> template) {
        serializeToNetwork(buffer, template.type(), template);
    }

    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeToNetwork(FriendlyByteBuf buffer, ArgumentTypeInfo<A, T> type, ArgumentTypeInfo.Template<A> template) {
        buffer.writeRegistryIdUnsafe(ForgeRegistries.COMMAND_ARGUMENT_TYPES, type);
        type.serializeToNetwork((T) template, buffer);
    }

    public static ArgumentTypeInfo.Template<?> deserialize(FriendlyByteBuf buffer) {
        var type = buffer.readRegistryIdUnsafe(ForgeRegistries.COMMAND_ARGUMENT_TYPES);
        Objects.requireNonNull(type, "Unknown argument type");
        return type.deserializeFromNetwork(buffer);
    }

    public static Component getMessage(Message message) {
        return message instanceof Component component ? component : Component.literal(message.getString());
    }

    public static Component getMessage(SimpleCommandExceptionType exception) {
        return getMessage(exception.create().getRawMessage());
    }
}
