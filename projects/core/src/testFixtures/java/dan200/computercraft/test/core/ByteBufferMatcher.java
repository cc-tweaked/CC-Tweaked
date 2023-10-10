// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.nio.ByteBuffer;

public final class ByteBufferMatcher extends TypeSafeMatcher<ByteBuffer> {
    private final ByteBuffer expected;

    private ByteBufferMatcher(ByteBuffer expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(ByteBuffer actual) {
        return expected.equals(actual);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expected);
    }

    @Override
    protected void describeMismatchSafely(ByteBuffer actual, Description mismatchDescription) {
        if (expected.remaining() != actual.remaining()) {
            mismatchDescription
                .appendValue(actual).appendText(" has ").appendValue(actual.remaining()).appendText(" bytes remaining");
            return;
        }

        var remaining = expected.remaining();
        var expectedPos = expected.position();
        var actualPos = actual.position();
        for (var i = 0; i < remaining; i++) {
            if (expected.get(expectedPos + i) == actual.get(actualPos + i)) continue;

            var offset = Math.max(i - 5, 0);
            var length = Math.min(i + 5, remaining - 1) - offset + 1;

            var expectedBytes = new byte[length];
            expected.duplicate().position(expectedPos + offset);
            expected.get(expectedBytes);

            var actualBytes = new byte[length];
            actual.duplicate().position(actualPos + offset);
            actual.get(actualBytes);

            mismatchDescription
                .appendText("failed at ").appendValue(i).appendText(System.lineSeparator())
                .appendText("expected ").appendValue(expectedBytes).appendText(System.lineSeparator())
                .appendText("was ").appendValue(actual);
            return;
        }
    }

    public static Matcher<ByteBuffer> bufferEqual(ByteBuffer buffer) {
        return new ByteBufferMatcher(buffer);
    }
}
