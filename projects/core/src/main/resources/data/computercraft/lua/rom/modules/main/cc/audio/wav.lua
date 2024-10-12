-- SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[-
Read WAV audio files into a table, including audio data.

WAV is a common file format used to store audio with metadata, including
information about the type of audio stored inside. WAV can store many different
types of codecs inside, including PCM and [DFPWM][`cc.audio.dfpwm`].

This module exposes a function to parse a WAV file into a table, [`readWAV`].
This function takes in the binary data from a WAV file, and outputs a more
usable table format with all the metadata and file audio inside. It also has a
[`readWAVFile`] function to simplify reading from a single file.

@see speaker.playAudio To play the chunks decoded by this module.
@since 1.113.0
@usage Reads "data/example.wav" into a table, prints its codec, sample rate,
and length in seconds, and plays the audio on a speaker.

```lua
local wav = require("cc.audio.wav")
local speaker = peripheral.find("speaker")

local audio = wav.readWAVFile("data/example.wav")
print("Codec type:", audio.codec)
print("Sample rate:", audio.sampleRate, "Hz")
-- audio.length is the length in samples; divide by sample rate to get seconds
print("Length:", audio.length / audio.sampleRate, "s")

for chunk in audio.read, 131072 do
  while not speaker.playAudio(chunk) do
    os.pullEvent("speaker_audio_empty")
  end
end
```
]]

local expect = require "cc.expect".expect
local dfpwm = require "cc.audio.dfpwm"

local str_unpack, str_sub, math_floor = string.unpack, string.sub, math.floor

local dfpwmUUID = "3ac1fa38-811d-4361-a40d-ce53ca607cd1" -- UUID for DFPWM in WAV files

local function uuidBytes(uuid) return uuid:gsub("-", ""):gsub("%x%x", function(c) return string.char(tonumber(c, 16)) end) end

local wavExtensible = {
    dfpwm = uuidBytes(dfpwmUUID),
    pcm = uuidBytes "01000000-0000-1000-8000-00aa00389b71",
    msadpcm = uuidBytes "02000000-0000-1000-8000-00aa00389b71",
    alaw = uuidBytes "06000000-0000-1000-8000-00aa00389b71",
    ulaw = uuidBytes "07000000-0000-1000-8000-00aa00389b71",
    adpcm = uuidBytes "11000000-0000-1000-8000-00aa00389b71",
    pcm_float = uuidBytes "03000000-0000-1000-8000-00aa00389b71",
}

local wavMetadata = {
    IPRD = "album",
    INAM = "title",
    IART = "artist",
    IWRI = "author",
    IMUS = "composer",
    IPRO = "producer",
    IPRT = "trackNumber",
    ITRK = "trackNumber",
    IFRM = "trackCount",
    PRT1 = "partNumber",
    PRT2 = "partCount",
    TLEN = "length",
    IRTD = "rating",
    ICRD = "date",
    ITCH = "encodedBy",
    ISFT = "encoder",
    ISRF = "media",
    IGNR = "genre",
    ICMT = "comment",
    ICOP = "copyright",
    ILNG = "language",
}

--[[- Read WAV data into a table.

The returned table contains the following fields:
- `codec`: A string with information about the codec used in the file (one of `u8`, `s16`, `s24`, `s32`, `f32`, `dfpwm`)
- `sampleRate`: The sample rate of the audio in Hz. If this is not 48000, the file will need to be resampled to play correctly.
- `channels`: The number of channels in the file (1 = mono, 2 = stereo).
- `length`: The number of samples in the file. Divide by sample rate to get seconds.
- `metadata`: If the WAV file contains `INFO` metadata, this table contains the metadata.
Known keys are converted to friendly names like `artist`, `album`, and `track`, while unknown keys are kept the same.
Otherwise, this table is empty.
- `read(length: number): number[]...`: This is a function that reads the audio data in chunks.
It takes the number of samples to read, and returns each channel chunk as multiple return values.
Channel data is in the same format as `speaker.playAudio` takes: 8-bit signed numbers.

@tparam string data The WAV data to read.
@treturn table The decoded WAV file data table.
]]
local function readWAV(data)
    expect(1, data, "string")
    local bitDepth, dataType, blockAlign
    local temp, pos = str_unpack("c4", data)
    if temp ~= "RIFF" then error("bad argument #1 (not a WAV file)", 2) end
    pos = pos + 4
    temp, pos = str_unpack("c4", data, pos)
    if temp ~= "WAVE" then error("bad argument #1 (not a WAV file)", 2) end
    local retval = { metadata = {} }
    while pos <= #data do
        local size
        temp, pos = str_unpack("c4", data, pos)
        size, pos = str_unpack("<I", data, pos)
        if temp == "fmt " then
            local chunk = str_sub(data, pos, pos + size - 1)
            pos = pos + size
            local format
            format, retval.channels, retval.sampleRate, blockAlign, bitDepth = str_unpack("<HHIxxxxHH", chunk)
            if format == 1 then
                dataType = bitDepth == 8 and "unsigned" or "signed"
                retval.codec = (bitDepth == 8 and "u" or "s") .. bitDepth
            elseif format == 3 then
                dataType = "float"
                retval.codec = "f32"
            elseif format == 0xFFFE then
                bitDepth = str_unpack("<H", chunk, 19)
                local uuid = str_sub(chunk, 25, 40)
                if uuid == wavExtensible.pcm then
                    dataType = bitDepth == 8 and "unsigned" or "signed"
                    retval.codec = (bitDepth == 8 and "u" or "s") .. bitDepth
                elseif uuid == wavExtensible.dfpwm then
                    dataType = "dfpwm"
                    retval.codec = "dfpwm"
                elseif uuid == wavExtensible.pcm_float then
                    dataType = "float"
                    retval.codec = "f32"
                else error("unsupported WAV file", 2) end
            else error("unsupported WAV file", 2) end
        elseif temp == "data" then
            local data = str_sub(data, pos, pos + size - 1)
            if #data < size then error("invalid WAV file", 2) end
            if not retval.length then retval.length = size / blockAlign end
            pos = pos + size
            local pos = 1
            local channels = retval.channels
            if dataType == "dfpwm" then
                local decoder = dfpwm.make_decoder()
                function retval.read(samples)
                    if pos > #data then return nil end
                    local chunk = decoder(str_sub(data, pos, pos + math.ceil(samples * channels / 8) - 1))
                    pos = pos + math.ceil(samples * channels / 8)
                    local res = {}
                    for i = 1, channels do
                        local c = {}
                        res[i] = c
                        for j = 1, samples do
                            c[j] = chunk[(j - 1) * channels + i]
                        end
                    end
                    return table.unpack(res)
                end
            else
                local format = dataType == "unsigned" and "I" .. (bitDepth / 8) or (dataType == "signed" and "i" .. (bitDepth / 8) or "f")
                local transform
                if dataType == "unsigned" then
                    function transform(n) return n - 128 end
                elseif dataType == "signed" then
                    if bitDepth == 16 then function transform(n) return math_floor(n / 0x100) end
                    elseif bitDepth == 24 then function transform(n) return math_floor(n / 0x10000) end
                    elseif bitDepth == 32 then function transform(n) return math_floor(n / 0x1000000) end end
                elseif dataType == "float" then
                    function transform(n) return math_floor(n * (n < 0 and 128 or 127)) end
                end
                function retval.read(samples)
                    if pos > #data then return nil end
                    local chunk = { ("<" .. format:rep(math.min(samples * channels, (#data - pos + 1) / (bitDepth / 8)))):unpack(data, pos) }
                    pos = table.remove(chunk)
                    local res = {}
                    for i = 1, channels do
                        local c = {}
                        res[i] = c
                        for j = 1, samples do
                            c[j] = transform(chunk[(j - 1) * channels + i])
                        end
                    end
                    return table.unpack(res)
                end
            end
        elseif temp == "fact" then
            retval.length, pos = str_unpack("<I4", data, pos)
        elseif temp == "LIST" then
            local type = str_unpack("c4", data, pos)
            if type == "INFO" then
                local e = pos + size
                pos = pos + 4
                while pos < e do
                    local str
                    type, str, pos = str_unpack("!2<c4s4Xh", data, pos)
                    str = str:gsub("\0+$", "")
                    if wavMetadata[type] then retval.metadata[wavMetadata[type]] = tonumber(str) or str
                    else retval.metadata[type] = tonumber(str) or str end
                end
            else pos = pos + size end
        else pos = pos + size end
    end
    if not retval.read then error("invalid WAV file", 2) end
    return retval
end

--- Reads a WAV file from a path.
--
-- This functions identically to [`readWAV`], but reads from a file instead.
--
-- @tparam string path The (absolute) path to read from.
-- @treturn table The decoded WAV file table.
-- @see readWAV To read WAV data from a string.
local function readWAVFile(path)
    expect(1, path, "string")
    local file = assert(fs.open(path, "rb"))
    local data = file.readAll()
    file.close()
    return readWAV(data)
end

return { readWAV = readWAV, readWAVFile = readWAVFile }
