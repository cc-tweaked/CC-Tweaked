// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api

/**
 * "Tags" associated with each test, denoting whether a specific test should be registered for the current Minecraft
 * session.
 *
 * This is used to only run some tests on the client, or when a specific mod is loaded.
 */
object TestTags {
    const val COMMON = "common"
    const val CLIENT = "client"

    private val tags: Set<String> = System.getProperty("cctest.tags", COMMON).split(',').toSet()

    fun isEnabled(tag: String) = tags.contains(tag)
}
