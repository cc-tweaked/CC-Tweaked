/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.core.apis.FSAPI
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveState
import dan200.computercraft.test.core.assertArrayEquals
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RedStoneWireBlock
import org.junit.jupiter.api.Assertions.assertEquals

class Disk_Drive_Test {
    /**
     * Ensure audio disks exist and we can play them.
     *
     * @see [#688](https://github.com/cc-tweaked/CC-Tweaked/issues/688)
     */
    @GameTest
    fun Audio_disk(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            callPeripheral("right", "hasAudio")
                .assertArrayEquals(true, message = "Disk has audio")

            callPeripheral("right", "getAudioTitle")
                .assertArrayEquals("C418 - 13", message = "Correct audio title")
        }
    }

    @GameTest
    fun Ejects_disk(helper: GameTestHelper) = helper.sequence {
        val stackAt = BlockPos(2, 2, 2)
        thenOnComputer { callPeripheral("right", "ejectDisk") }
        thenWaitUntil { helper.assertItemEntityPresent(Items.MUSIC_DISC_13, stackAt, 0.0) }
    }

    @GameTest
    fun Adds_removes_mount(helper: GameTestHelper) = helper.sequence {
        thenIdle(2)
        thenOnComputer {
            getApi<FSAPI>().getDrive("disk").assertArrayEquals("right")
            callPeripheral("right", "ejectDisk")
        }
        thenIdle(2)
        thenOnComputer { assertEquals(null, getApi<FSAPI>().getDrive("disk")) }
    }

    /**
     * Check comparators can read the contents of the disk drive
     */
    @GameTest
    fun Comparator(helper: GameTestHelper) = helper.sequence {
        val drivePos = BlockPos(2, 2, 2)
        val dustPos = BlockPos(2, 2, 4)

        // Adding items should provide power
        thenExecute {
            val drive = helper.getBlockEntity(drivePos, ModRegistry.BlockEntities.DISK_DRIVE.get())
            drive.setItem(0, ItemStack(ModRegistry.Items.TREASURE_DISK.get()))
            drive.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 15) }

        // And removing them should reset power.
        thenExecute {
            val drive = helper.getBlockEntity(drivePos, ModRegistry.BlockEntities.DISK_DRIVE.get())
            drive.setItem(0, ItemStack.EMPTY)
            drive.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 0) }
    }

    /**
     * Changing the inventory contents updates the block state
     */
    @GameTest
    fun Contents_updates_state(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val drive = helper.getBlockEntity(pos, ModRegistry.BlockEntities.DISK_DRIVE.get())

            drive.setItem(0, ItemStack(Items.DIRT))
            drive.setChanged()
            helper.assertBlockHas(pos, DiskDriveBlock.STATE, DiskDriveState.INVALID)

            drive.setItem(0, ItemStack(ModRegistry.Items.TREASURE_DISK.get()))
            drive.setChanged()
            helper.assertBlockHas(pos, DiskDriveBlock.STATE, DiskDriveState.FULL)

            drive.setItem(0, ItemStack.EMPTY)
            drive.setChanged()
            helper.assertBlockHas(pos, DiskDriveBlock.STATE, DiskDriveState.EMPTY)
        }
    }

    /**
     * When the block is broken, we drop the contents and an optionally named stack.
     */
    @GameTest
    fun Drops_contents(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.level.destroyBlock(helper.absolutePos(BlockPos(2, 2, 2)), true)
            helper.assertExactlyItems(
                ItemStack(ModRegistry.Items.DISK_DRIVE.get()).setHoverName(Component.literal("My Disk Drive")),
                ItemStack(ModRegistry.Items.TREASURE_DISK.get()),
                message = "Breaking a disk drive should drop the contents",
            )
        }
    }
}
