// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;

public final class Exceptions {
    public static final DynamicCommandExceptionType COMPUTER_ARG_NONE = translated1("argument.computercraft.computer.no_matching");
    public static final Dynamic2CommandExceptionType COMPUTER_ARG_MANY = translated2("argument.computercraft.computer.many_matching");

    public static final DynamicCommandExceptionType TRACKING_FIELD_ARG_NONE = translated1("argument.computercraft.tracking_field.no_field");

    static final SimpleCommandExceptionType NOT_TRACKING_EXCEPTION = translated("commands.computercraft.track.stop.not_enabled");
    static final SimpleCommandExceptionType NO_TIMINGS_EXCEPTION = translated("commands.computercraft.track.dump.no_timings");

    public static final SimpleCommandExceptionType ARGUMENT_EXPECTED = translated("argument.computercraft.argument_expected");

    public static final DynamicCommandExceptionType UNKNOWN_FAMILY = translated1("argument.computercraft.unknown_computer_family");

    public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE = EntitySelectorParser.ERROR_EXPECTED_OPTION_VALUE;
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS = EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS;
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = EntitySelectorOptions.ERROR_UNKNOWN_OPTION;
    public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = EntitySelectorOptions.ERROR_INAPPLICABLE_OPTION;

    private static SimpleCommandExceptionType translated(String key) {
        return new SimpleCommandExceptionType(Component.translatable(key));
    }

    private static DynamicCommandExceptionType translated1(String key) {
        return new DynamicCommandExceptionType(x -> Component.translatable(key, x));
    }

    private static Dynamic2CommandExceptionType translated2(String key) {
        return new Dynamic2CommandExceptionType((x, y) -> Component.translatable(key, x, y));
    }
}
