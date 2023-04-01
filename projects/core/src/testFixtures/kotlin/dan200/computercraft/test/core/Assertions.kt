// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsArray
import org.junit.jupiter.api.Assertions

/** Postfix version of [Assertions.assertArrayEquals] */
fun Array<out Any?>?.assertArrayEquals(vararg expected: Any?, message: String? = null) {
    assertThat(
        message ?: "",
        this,
        IsArrayVerbose(expected.map { FuzzyEqualTo(it) }.toTypedArray()),
    )
}

/**
 * Extension of [IsArray] which always prints the array, not just when the items are mismatched.
 */
internal class IsArrayVerbose<T>(private val elementMatchers: Array<Matcher<in T>>) : IsArray<T>(elementMatchers) {
    override fun describeMismatchSafely(actual: Array<out T>, description: Description) {
        description.appendText("array was ").appendValue(actual)
        if (actual.size != elementMatchers.size) {
            description.appendText(" with length ").appendValue(actual.size)
            return
        }

        for (i in actual.indices) {
            if (!elementMatchers[i].matches(actual[i])) {
                description.appendText("with element ").appendValue(i).appendText(" ")
                elementMatchers[i].describeMismatch(actual[i], description)
                return
            }
        }
    }
}

/**
 * An equality matcher which is slightly more relaxed on comparing some values.
 */
internal class FuzzyEqualTo(private val expected: Any?) : BaseMatcher<Any?>() {
    override fun describeTo(description: Description) {
        description.appendValue(expected)
    }

    override fun matches(actual: Any?): Boolean {
        if (actual == null) return false

        if (actual is Number && expected is Number && actual.javaClass != expected.javaClass) {
            // Allow equating integers and floats.
            return actual.toDouble() == expected.toDouble()
        }

        return actual == expected
    }
}
