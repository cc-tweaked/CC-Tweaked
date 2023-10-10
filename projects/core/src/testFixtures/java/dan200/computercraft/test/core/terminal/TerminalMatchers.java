// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.terminal;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.test.core.ContramapMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.Arrays;

public class TerminalMatchers {
    public static Matcher<Terminal> textColourMatches(String[] x) {
        return linesMatch("text colour", Terminal::getTextColourLine, x);
    }

    public static Matcher<Terminal> backgroundColourMatches(String[] x) {
        return linesMatch("background colour", Terminal::getBackgroundColourLine, x);
    }

    public static Matcher<Terminal> textMatches(String[] x) {
        return linesMatch("text", Terminal::getLine, x);
    }

    @SuppressWarnings("unchecked")
    public static Matcher<Terminal> linesMatch(String kind, LineProvider getLine, String[] lines) {
        return linesMatchWith(kind, getLine, Arrays.stream(lines).map(Matchers::equalTo).toArray(Matcher[]::new));
    }

    public static Matcher<Terminal> linesMatchWith(String kind, LineProvider getLine, Matcher<String>[] lines) {
        return ContramapMatcher.contramap(Matchers.array(lines), kind, terminal -> {
            var termLines = new String[terminal.getHeight()];
            for (var i = 0; i < termLines.length; i++) termLines[i] = getLine.getLine(terminal, i).toString();
            return termLines;
        });
    }

    @FunctionalInterface
    public interface LineProvider {
        TextBuffer getLine(Terminal terminal, int line);
    }

}
