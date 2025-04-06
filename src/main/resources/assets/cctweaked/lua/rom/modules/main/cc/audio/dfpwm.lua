-- SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[-
Convert between streams of DFPWM audio data and a list of amplitudes.

DFPWM (Dynamic Filter Pulse Width Modulation) is an audio codec designed by GreaseMonkey. It's a relatively compact
format compared to raw PCM data, only using 1 bit per sample, but is simple enough to encode and decode in real time.

Typically DFPWM audio is read from [the filesystem][`fs.ReadHandle`] or a [a web request][`http.Response`] as a string,
and converted a format suitable for [`speaker.playAudio`].

## Encoding and decoding files
This module exposes two key functions, [`make_decoder`] and [`make_encoder`], which construct a new decoder or encoder.
The returned encoder/decoder is itself a function, which converts between the two kinds of data.

These encoders and decoders have lots of hidden state, so you should be careful to use the same encoder or decoder for
a specific audio stream. Typically you will want to create a decoder for each stream of audio you read, and an encoder
for each one you write.

## Converting audio to DFPWM
DFPWM is not a popular file format and so standard audio processing tools may not have an option to export to it.
Instead, you can convert audio files online using [music.madefor.cc], the [LionRay Wav Converter][LionRay] Java
application or [FFmpeg] 5.1 or later.

[music.madefor.cc]: https://music.madefor.cc/ "DFPWM audio converter for Computronics and CC: Tweaked"
[LionRay]: https://github.com/gamax92/LionRay/ "LionRay Wav Converter "
[FFmpeg]: https://ffmpeg.org "FFmpeg command-line audio manipulation library"

@see guide!speaker_audio Gives a more general introduction to audio processing and the speaker.
@see speaker.playAudio To play the decoded audio data.
@since 1.100.0
@usage Reads "data/example.dfpwm" in chunks, decodes them and then doubles the speed of the audio. The resulting audio
is then re-encoded and saved to "speedy.dfpwm". This processed audio can then be played with the `speaker` program.

```lua
local dfpwm = require("cc.audio.dfpwm")

local encoder = dfpwm.make_encoder()
local decoder = dfpwm.make_decoder()

local out = fs.open("speedy.dfpwm", "wb")
for input in io.lines("data/example.dfpwm", 16 * 1024 * 2) do
  local decoded = decoder(input)
  local output = {}

  -- Read two samples at once and take the average.
  for i = 1, #decoded, 2 do
    local value_1, value_2 = decoded[i], decoded[i + 1]
    output[(i + 1) / 2] = (value_1 + value_2) / 2
  end

  out.write(encoder(output))

  sleep(0) -- This program takes a while to run, so we need to make sure we yield.
end
out.close()
```
]]

local expect = require "cc.expect".expect

local char, byte, floor, band, rshift = string.char, string.byte, math.floor, bit32.band, bit32.arshift

local PREC = 10
local PREC_POW = 2 ^ PREC
local PREC_POW_HALF = 2 ^ (PREC - 1)
local STRENGTH_MIN = 2 ^ (PREC - 8 + 1)

local function make_predictor()
    local charge, strength, previous_bit = 0, 0, false

    return function(current_bit)
        local target = current_bit and 127 or -128

        local next_charge = charge + floor((strength * (target - charge) + PREC_POW_HALF) / PREC_POW)
        if next_charge == charge and next_charge ~= target then
            next_charge = next_charge + (current_bit and 1 or -1)
        end

        local z = current_bit == previous_bit and PREC_POW - 1 or 0
        local next_strength = strength
        if next_strength ~= z then next_strength = next_strength + (current_bit == previous_bit and 1 or -1) end
        if next_strength < STRENGTH_MIN then next_strength = STRENGTH_MIN end

        charge, strength, previous_bit = next_charge, next_strength, current_bit
        return charge
    end
end

--[[- Create a new encoder for converting PCM audio data into DFPWM.

The returned encoder is itself a function. This function accepts a table of amplitude data between -128 and 127 and
returns the encoded DFPWM data.

> [Reusing encoders][!WARNING]
> Encoders have lots of internal state which tracks the state of the current stream. If you reuse an encoder for multiple
> streams, or use different encoders for the same stream, the resulting audio may not sound correct.

@treturn function(pcm: { number... }):string The encoder function
@see encode A helper function for encoding an entire file of audio at once.
]]
local function make_encoder()
    local predictor = make_predictor()
    local previous_charge = 0

    return function(input)
        expect(1, input, "table")

        local output, output_n = {}, 0
        for i = 1, #input, 8 do
            local this_byte = 0
            for j = 0, 7 do
                local inp_charge = floor(input[i + j] or 0)
                if inp_charge > 127 or inp_charge < -128 then
                    error(("Amplitude at position %d was %d, but should be between -128 and 127"):format(i + j, inp_charge), 2)
                end

                local current_bit = inp_charge > previous_charge or (inp_charge == previous_charge and inp_charge == 127)
                this_byte = floor(this_byte / 2) + (current_bit and 128 or 0)

                previous_charge = predictor(current_bit)
            end

            output_n = output_n + 1
            output[output_n] = char(this_byte)
        end

        return table.concat(output, "", 1, output_n)
    end
end

--[[- Create a new decoder for converting DFPWM into PCM audio data.

The returned decoder is itself a function. This function accepts a string and returns a table of amplitudes, each value
between -128 and 127.

> [Reusing decoders][!WARNING]
> Decoders have lots of internal state which tracks the state of the current stream. If you reuse an decoder for
> multiple streams, or use different decoders for the same stream, the resulting audio may not sound correct.

@treturn function(dfpwm: string):{ number... } The encoder function
@see decode A helper function for decoding an entire file of audio at once.

@usage Reads "data/example.dfpwm" in blocks of 16KiB (the speaker can accept a maximum of 128×1024 samples), decodes
them and then plays them through the speaker.

```lua {data-peripheral=speaker}
local dfpwm = require "cc.audio.dfpwm"
local speaker = peripheral.find("speaker")

local decoder = dfpwm.make_decoder()
for input in io.lines("data/example.dfpwm", 16 * 1024) do
  local decoded = decoder(input)
  while not speaker.playAudio(decoded) do
    os.pullEvent("speaker_audio_empty")
  end
end
```
]]
local function make_decoder()
    local predictor = make_predictor()
    local low_pass_charge = 0
    local previous_charge, previous_bit = 0, false

    return function (input)
        expect(1, input, "string")

        local output, output_n = {}, 0
        for i = 1, #input do
            local input_byte = byte(input, i)
            for _ = 1, 8 do
                local current_bit = band(input_byte, 1) ~= 0
                local charge = predictor(current_bit)

                local antijerk = charge
                if current_bit ~= previous_bit then
                    antijerk = floor((charge + previous_charge + 1) / 2)
                end

                previous_charge, previous_bit = charge, current_bit

                low_pass_charge = low_pass_charge + floor(((antijerk - low_pass_charge) * 140 + 0x80) / 256)

                output_n = output_n + 1
                output[output_n] = low_pass_charge

                input_byte = rshift(input_byte, 1)
            end
        end

        return output
    end
end

--[[- A convenience function for decoding a complete file of audio at once.

This should only be used for short files. For larger files, one should read the file in chunks and process it using
[`make_decoder`].

@tparam string input The DFPWM data to convert.
@treturn { number... } The produced amplitude data.
@see make_decoder
]]
local function decode(input)
    expect(1, input, "string")
    return make_decoder()(input)
end

--[[- A convenience function for encoding a complete file of audio at once.

This should only be used for complete pieces of audio. If you are writing multiple chunks to the same place,
you should use an encoder returned by [`make_encoder`] instead.

@tparam { number... } input The table of amplitude data.
@treturn string The encoded DFPWM data.
@see make_encoder
]]
local function encode(input)
    expect(1, input, "table")
    return make_encoder()(input)
end

return {
    make_encoder = make_encoder,
    encode = encode,

    make_decoder = make_decoder,
    decode = decode,
}
