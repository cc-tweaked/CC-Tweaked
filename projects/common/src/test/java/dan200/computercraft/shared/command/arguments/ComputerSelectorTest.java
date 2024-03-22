// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dan200.computercraft.shared.command.Exceptions;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.test.core.ReplaceUnderscoresDisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(ReplaceUnderscoresDisplayNameGenerator.class)
class ComputerSelectorTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentTestCases")
    public void Parse_basic_inputs(String input, ComputerSelector expected) throws CommandSyntaxException {
        assertEquals(expected, ComputerSelector.parse(new StringReader(input)));
    }

    public static Arguments[] getArgumentTestCases() {
        return new Arguments[]{
            // Legacy selectors
            Arguments.of("@some_label", new ComputerSelector("@some_label", OptionalInt.empty(), null, OptionalInt.empty(), "some_label", null, null, null)),
            Arguments.of("~normal", new ComputerSelector("~normal", OptionalInt.empty(), null, OptionalInt.empty(), null, ComputerFamily.NORMAL, null, null)),
            Arguments.of("#123", new ComputerSelector("#123", OptionalInt.empty(), null, OptionalInt.of(123), null, null, null, null)),
            Arguments.of("123", new ComputerSelector("123", OptionalInt.of(123), null, OptionalInt.empty(), null, null, null, null)),
            // New selectors
            Arguments.of("@c[]", new ComputerSelector("@c[]", OptionalInt.empty(), null, OptionalInt.empty(), null, null, null, null)),
            Arguments.of("@c[instance=5e18f505-62f7-46f8-83f3-792f03224724]", new ComputerSelector("@c[instance=5e18f505-62f7-46f8-83f3-792f03224724]", OptionalInt.empty(), UUID.fromString("5e18f505-62f7-46f8-83f3-792f03224724"), OptionalInt.empty(), null, null, null, null)),
            Arguments.of("@c[id=123]", new ComputerSelector("@c[id=123]", OptionalInt.empty(), null, OptionalInt.of(123), null, null, null, null)),
            Arguments.of("@c[label=\"foo\"]", new ComputerSelector("@c[label=\"foo\"]", OptionalInt.empty(), null, OptionalInt.empty(), "foo", null, null, null)),
            Arguments.of("@c[family=normal]", new ComputerSelector("@c[family=normal]", OptionalInt.empty(), null, OptionalInt.empty(), null, ComputerFamily.NORMAL, null, null)),
            // Complex selectors
            Arguments.of("@c[ id = 123 , ]", new ComputerSelector("@c[ id = 123 , ]", OptionalInt.empty(), null, OptionalInt.of(123), null, null, null, null)),
            Arguments.of("@c[id=123,family=normal]", new ComputerSelector("@c[id=123,family=normal]", OptionalInt.empty(), null, OptionalInt.of(123), null, ComputerFamily.NORMAL, null, null)),
        };
    }

    @Test
    public void Fails_on_repeated_options() {
        var error = assertThrows(CommandSyntaxException.class, () -> ComputerSelector.parse(new StringReader("@c[id=1, id=2]")));
        assertEquals(Exceptions.ERROR_INAPPLICABLE_OPTION, error.getType());
    }

    @Test
    public void Complete_selector_components() {
        assertEquals(List.of(new Suggestion(StringRange.between(0, 1), "@c[")), suggest("@"));
        assertThat(suggest("@c["), hasItem(
            new Suggestion(StringRange.at(3), "family", ComputerSelector.options().get("family").tooltip())
        ));
        assertEquals(List.of(new Suggestion(StringRange.at(9), "=")), suggest("@c[family"));
        assertEquals(List.of(new Suggestion(StringRange.at(16), ",")), suggest("@c[family=normal"));
    }

    @Test
    public void Complete_selector_family() {
        assertThat(suggest("@c[family="), containsInAnyOrder(
            new Suggestion(StringRange.at(10), "normal"),
            new Suggestion(StringRange.at(10), "advanced"),
            new Suggestion(StringRange.at(10), "command")
        ));
        assertThat(suggest("@c[family=n"), contains(
            new Suggestion(StringRange.between(10, 11), "normal")
        ));
    }

    private List<Suggestion> suggest(String input) {
        var context = new CommandContext<>(new Object(), "", Map.of(), null, null, List.of(), StringRange.at(0), null, null, false);
        return ComputerSelector.suggest(context, new SuggestionsBuilder(input, 0)).getNow(null).getList();
    }
}
