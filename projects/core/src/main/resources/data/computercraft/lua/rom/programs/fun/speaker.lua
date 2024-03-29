-- SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local function get_speakers(name)
    if name then
        local speaker = peripheral.wrap(name)
        if speaker == nil then
            error(("Speaker %q does not exist"):format(name), 0)
            return
        elseif not peripheral.hasType(name, "speaker") then
            error(("%q is not a speaker"):format(name), 0)
        end

        return { speaker }
    else
        local speakers = { peripheral.find("speaker") }
        if #speakers == 0 then
            error("No speakers attached", 0)
        end
        return speakers
    end
end

local function pcm_decoder(chunk)
    local buffer = {}
    for i = 1, #chunk do
        buffer[i] = chunk:byte(i) - 128
    end
    return buffer
end

local function report_invalid_format(format)
    printError(("speaker cannot play %s files."):format(format))
    local pp = require "cc.pretty"
    pp.print("Run '" .. pp.text("help speaker", colours.lightGrey) .. "' for information on supported formats.")
end


local cmd = ...
if cmd == "stop" then
    local _, name = ...
    for _, speaker in pairs(get_speakers(name)) do speaker.stop() end
elseif cmd == "play" then
    local _, file, name = ...
    if not file then
        error("Usage: speaker play <file or url> [speaker]", 0)
    end

    local speaker = get_speakers(name)[1]

    local handle, err
    if http and file:match("^https?://") then
        print("Downloading...")
        handle, err = http.get(file)
    else
        handle, err = fs.open(shell.resolve(file), "r")
    end

    if not handle then
        printError("Could not play audio:")
        error(err, 0)
    end

    local start = handle.read(4)
    local pcm = false
    local size = 16 * 1024 - 4
    if start == "RIFF" then
        handle.read(4)
        if handle.read(8) ~= "WAVEfmt " then
            handle.close()
            error("Could not play audio: Unsupported WAV file", 0)
        end

        local fmtsize = ("<I4"):unpack(handle.read(4))
        local fmt = handle.read(fmtsize)
        local format, channels, rate, _, _, bits = ("<I2I2I4I4I2I2"):unpack(fmt)
        if not ((format == 1 and bits == 8) or (format == 0xFFFE and bits == 1)) then
            handle.close()
            error("Could not play audio: Unsupported WAV file", 0)
        end
        if channels ~= 1 or rate ~= 48000 then
            print("Warning: Only 48 kHz mono WAV files are supported. This file may not play correctly.")
        end
        if format == 0xFFFE then
            local guid = fmt:sub(25)
            if guid ~= "\x3A\xC1\xFA\x38\x81\x1D\x43\x61\xA4\x0D\xCE\x53\xCA\x60\x7C\xD1" then -- DFPWM format GUID
                handle.close()
                error("Could not play audio: Unsupported WAV file", 0)
            end
            size = size + 4
        else
            pcm = true
            size = 16 * 1024 * 8
        end

        repeat
            local chunk = handle.read(4)
            if chunk == nil then
                handle.close()
                error("Could not play audio: Invalid WAV file", 0)
            elseif chunk ~= "data" then -- Ignore extra chunks
                local size = ("<I4"):unpack(handle.read(4))
                handle.read(size)
            end
        until chunk == "data"

        handle.read(4)
        start = nil
    -- Detect several other common audio files.
    elseif start == "OggS" then return report_invalid_format("Ogg")
    elseif start == "fLaC" then return report_invalid_format("FLAC")
    elseif start:sub(1, 3) == "ID3" then return report_invalid_format("MP3")
    elseif start == "<!DO" --[[<!DOCTYPE]] then return report_invalid_format("HTML")
    end

    print("Playing " .. file)

    local decoder = pcm and pcm_decoder or require "cc.audio.dfpwm".make_decoder()
    while true do
        local chunk = handle.read(size)
        if not chunk then break end
        if start then
            chunk, start = start .. chunk, nil
            size = size + 4
        end

        local buffer = decoder(chunk)
        while not speaker.playAudio(buffer) do
            os.pullEvent("speaker_audio_empty")
        end
    end

    handle.close()
elseif cmd == "sound" then
    local _, sound, volume, pitch, name = ...

    if not sound then
        error("Usage: speaker sound <sound> [volume] [pitch] [speaker]", 0)
        return
    end

    if volume then
        volume = tonumber(volume)
        if not volume then
            error("Volume must be a number", 0)
        end
        if volume < 0 or volume > 3 then
            error("Volume must be between 0 and 3", 0)
        end
    end

    if pitch then
        pitch = tonumber(pitch)
        if not pitch then
            error("Pitch must be a number", 0)
        end
        if pitch < 0 or pitch > 2 then
            error("Pitch must be between 0 and 2", 0)
        end
    end

    local speaker = get_speakers(name)[1]

    if speaker.playSound(sound, volume, pitch) then
        print(("Played sound %q on speaker %q with volume %s and pitch %s."):format(
            sound, peripheral.getName(speaker), volume or 1, pitch or 1
        ))
    else
        error(("Could not play sound %q"):format(sound), 0)
    end
else
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage:")
    print(programName .. " play <file or url> [speaker]")
    print(programName .. " sound <sound> [volume] [pitch] [speaker]")
    print(programName .. " stop [speaker]")
end
