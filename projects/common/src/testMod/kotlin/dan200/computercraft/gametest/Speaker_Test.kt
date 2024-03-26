// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.gametest.api.thenOnComputer
import dan200.computercraft.gametest.api.tryMultipleTimes
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral
import dan200.computercraft.test.core.assertArrayEquals
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.sounds.SoundEvents

class Speaker_Test {
    /**
     * [SpeakerPeripheral.playSound] fails if there is already a sound queued.
     */
    @GameTest
    fun Fails_to_play_multiple_sounds(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            callPeripheral("right", "playSound", SoundEvents.NOTE_BLOCK_HARP.key().location().toString())
                .assertArrayEquals(true)

            tryMultipleTimes(2) { // We could technically call this a tick later, so try twice
                callPeripheral("right", "playSound", SoundEvents.NOTE_BLOCK_HARP.key().location().toString())
                    .assertArrayEquals(false)
            }
        }
    }

    /**
     * [SpeakerPeripheral.playSound] will not play records.
     */
    @GameTest
    fun Will_not_play_record(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            callPeripheral("right", "playSound", SoundEvents.MUSIC_DISC_PIGSTEP.location.toString())
                .assertArrayEquals(false)
        }
    }
}
