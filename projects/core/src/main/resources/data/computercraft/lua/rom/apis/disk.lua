-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Interact with disk drives.

These functions can operate on locally attached or remote disk drives. To use a
locally attached drive, specify “side” as one of the six sides (e.g. `left`); to
use a remote disk drive, specify its name as printed when enabling its modem
(e.g. `drive_0`).

> [!TIP]
> All computers (except command computers), turtles and pocket computers can be
> placed within a disk drive to access it's internal storage like a disk.

@module disk
@since 1.2
]]

local function isDrive(name)
    if type(name) ~= "string" then
        error("bad argument #1 (string expected, got " .. type(name) .. ")", 3)
    end
    return peripheral.getType(name) == "drive"
end

--- Checks whether any item at all is in the disk drive
--
-- @tparam string name The name of the disk drive.
-- @treturn boolean If something is in the disk drive.
-- @usage disk.isPresent("top")
function isPresent(name)
    if isDrive(name) then
        return peripheral.call(name, "isDiskPresent")
    end
    return false
end

--- Get the label of the floppy disk, record, or other media within the given
-- disk drive.
--
-- If there is a computer or turtle within the drive, this will set the label as
-- read by `os.getComputerLabel`.
--
-- @tparam string name The name of the disk drive.
-- @treturn string|nil The name of the current media, or `nil` if the drive is
-- not present or empty.
-- @see disk.setLabel
function getLabel(name)
    if isDrive(name) then
        return peripheral.call(name, "getDiskLabel")
    end
    return nil
end

--- Set the label of the floppy disk or other media
--
-- @tparam string name The name of the disk drive.
-- @tparam string|nil label The new label of the disk
function setLabel(name, label)
    if isDrive(name) then
        peripheral.call(name, "setDiskLabel", label)
    end
end

--- Check whether the current disk provides a mount.
--
-- This will return true for disks and computers, but not records.
--
-- @tparam string name The name of the disk drive.
-- @treturn boolean If the disk is present and provides a mount.
-- @see disk.getMountPath
function hasData(name)
    if isDrive(name) then
        return peripheral.call(name, "hasData")
    end
    return false
end

--- Find the directory name on the local computer where the contents of the
-- current floppy disk (or other mount) can be found.
--
-- @tparam string name The name of the disk drive.
-- @treturn string|nil The mount's directory, or `nil` if the drive does not
-- contain a floppy or computer.
-- @see disk.hasData
function getMountPath(name)
    if isDrive(name) then
        return peripheral.call(name, "getMountPath")
    end
    return nil
end

--- Whether the current disk is a [music disk][disk] as opposed to a floppy disk
-- or other item.
--
-- If this returns true, you will can [play][`disk.playAudio`] the record.
--
-- [disk]: https://minecraft.wiki/w/Music_Disc
--
-- @tparam string name The name of the disk drive.
-- @treturn boolean If the disk is present and has audio saved on it.
function hasAudio(name)
    if isDrive(name) then
        return peripheral.call(name, "hasAudio")
    end
    return false
end

--- Get the title of the audio track from the music record in the drive.
--
-- This generally returns the same as [`disk.getLabel`] for records.
--
-- @tparam string name The name of the disk drive.
-- @treturn string|false|nil The track title, [`false`] if there is not a music
-- record in the drive or `nil` if no drive is present.
function getAudioTitle(name)
    if isDrive(name) then
        return peripheral.call(name, "getAudioTitle")
    end
    return nil
end

--- Starts playing the music record in the drive.
--
-- If any record is already playing on any disk drive, it stops before the
-- target drive starts playing. The record stops when it reaches the end of the
-- track, when it is removed from the drive, when [`disk.stopAudio`] is called, or
-- when another record is started.
--
-- @tparam string name The name of the disk drive.
-- @usage disk.playAudio("bottom")
function playAudio(name)
    if isDrive(name) then
        peripheral.call(name, "playAudio")
    end
end

--- Stops the music record in the drive from playing, if it was started with
-- [`disk.playAudio`].
--
-- @tparam string name The name o the disk drive.
function stopAudio(name)
    if not name then
        for _, sName in ipairs(peripheral.getNames()) do
            stopAudio(sName)
        end
    else
        if isDrive(name) then
            peripheral.call(name, "stopAudio")
        end
    end
end

--- Ejects any item currently in the drive, spilling it into the world as a loose item.
--
-- @tparam string name The name of the disk drive.
-- @usage disk.eject("bottom")
function eject(name)
    if isDrive(name) then
        peripheral.call(name, "ejectDisk")
    end
end

--- Returns a number which uniquely identifies the disk in the drive.
--
-- Note, unlike [`disk.getLabel`], this does not return anything for other media,
-- such as computers or turtles.
--
-- @tparam string name The name of the disk drive.
-- @treturn string|nil The disk ID, or `nil` if the drive does not contain a floppy disk.
-- @since 1.4
function getID(name)
    if isDrive(name) then
        return peripheral.call(name, "getDiskID")
    end
    return nil
end
