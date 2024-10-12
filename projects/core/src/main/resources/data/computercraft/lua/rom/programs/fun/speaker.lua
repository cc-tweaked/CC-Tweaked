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
    local wav = false
    local size = 16 * 1024 - 4
    if start == "RIFF" then
        local data = start .. handle.readAll()
        handle.close()
        local ok
        ok, handle = pcall(require("cc.audio.wav").readWAV, data)
        if not ok then
            printError("Could not play audio:")
            error(err, 0)
        end
        wav = true
        start = nil
        if handle.sampleRate ~= 48000 then error("Could not play audio: Unsupported sample rate") end
        if handle.channels ~= 1 then printError("This audio file has more than one channel. It may not play correctly.") end
    -- Detect several other common audio files.
    elseif start == "OggS" then return report_invalid_format("Ogg")
    elseif start == "fLaC" then return report_invalid_format("FLAC")
    elseif start:sub(1, 3) == "ID3" then return report_invalid_format("MP3")
    elseif start == "<!DO" --[[<!DOCTYPE]] then return report_invalid_format("HTML")
    end

    if handle.metadata and handle.metadata.title and handle.metadata.artist then
        print("Playing " .. handle.metadata.artist .. " - " .. handle.metadata.title)
    else
        print("Playing " .. file)
    end

    local decoder = wav and function(c) return c end or require "cc.audio.dfpwm".make_decoder()
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
