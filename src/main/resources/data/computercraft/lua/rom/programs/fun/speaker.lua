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


local cmd = ...
if cmd == "stop" then
    local _, name = ...
    for _, speaker in pairs(get_speakers(name)) do speaker.stop() end
elseif cmd == "play" then
    local _, file, name = ...
    local speaker = get_speakers(name)[1]

    local handle, err
    if http and file:match("^https?://") then
        print("Downloading...")
        handle, err = http.get{ url = file, binary = true }
    else
        handle, err = fs.open(file, "rb")
    end

    if not handle then
        printError("Could not play audio:")
        error(err, 0)
    end

    print("Playing " .. file)

    local decoder = require "cc.audio.dfpwm".make_decoder()
    while true do
        local chunk = handle.read(16 * 1024)
        if not chunk then break end

        local buffer = decoder(chunk)
        while not speaker.playAudio(buffer) do
            os.pullEvent("speaker_audio_empty")
        end
    end

    handle.close()
else
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage:")
    print(programName .. " play <file or url> [speaker]")
    print(programName .. " stop [speaker]")
end
