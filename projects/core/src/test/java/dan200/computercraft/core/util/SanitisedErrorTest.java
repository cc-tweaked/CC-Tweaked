// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.CharSequenceLength.hasLength;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitisedErrorTest {
    private void assertEquivalent(Throwable t) {
        var truncatedOutput = new SanitisedError(t).toString();

        var writer = new StringWriter();
        try (var printWriter = new PrintWriter(writer)) {
            t.printStackTrace(printWriter);
        }
        var actualOutput = writer.toString().replace("\r\n", "\n");

        assertEquals(actualOutput, truncatedOutput);
    }

    @Test
    public void testBasicException() {
        assertEquivalent(new RuntimeException("A problem occurred"));
    }

    @Test
    public void textExceptionWithCause() {
        var inner = new RuntimeException("Inner error");
        var outer = new RuntimeException("Outer error", inner);
        assertEquivalent(outer);
    }

    @Test
    public void textExceptionWithSuppressed() {
        var inner = new RuntimeException("Inner error");
        var outer = new RuntimeException("Outer error");
        outer.addSuppressed(inner);
        assertEquivalent(outer);
    }

    @Test
    public void testTruncates() {
        var error = new RuntimeException("Some message".repeat(100));
        error.setStackTrace(new StackTraceElement[0]);

        var message = new SanitisedError(error).toString();
        assertThat(message, containsString("message truncated"));
        assertThat(message, hasLength(lessThanOrEqualTo(250)));
    }

    @Test
    public void testStrips() {
        var error = new RuntimeException("Some message\n\r\t\033");
        error.setStackTrace(new StackTraceElement[0]);

        var message = new SanitisedError(error).toString();
        assertThat(message, startsWith("java.lang.RuntimeException: Some message\\n\\r\\t\\u{1b}\n"));
    }
}
