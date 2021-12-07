---
module: [kind=guide] speaker_audio
see: speaker.playAudio Play PCM audio using a speaker.
see: cc.audio.dfpwm Provides utilities for encoding and decoding DFPWM files.
---

# Playing audio with speakers
ComputerCraft's speaker peripheral provides a powerful way to play any audio you like: @{speaker.playAudio}. However,
for those people unfamiliar with digital audio, it's not the most familiar thing to use. This guide provides an
introduction to digital audio, demonstrates how to play music with ComputerCraft's speakers, and then briefly discusses
the more complex topic of audio processing.

## A short introduction to digital audio
When sound is recorded it comes in as an analogue signal, effectively the electrical version of a sound wave. However,
this signal is continuous, and so can't be used directly by a computer. Instead, we measure (or *sample*) the amplitude
of the wave many times a second and then *quantise* that amplitude, rounding it to the nearest representable value.

This representation of sound - a long, uniformally sampled list of amplitudes is referred to as [Pulse-code Modulation][PCM]
(PCM). PCM can be thought of as the "standard" audio format, as it's incredibly easy to work with. For instance, to
mix two pieces of audio together, you can just take the average of every sample.

ComputerCraft's speakers also work with PCM audio. It plays back 48,000 samples a second, where each sample is an
integer between -128 and 127. This is more commonly referred to as 48kHz and an 8-bit resolution.

Let's now look at a quick example. We're going to generate a [Sine Wave] at 220Hz, which sounds like a low monotonous
hum. First we wrap our speaker, and then we fill our buffer with 128k samples - this is the maximum number of samples
a speaker can accept in one go.

In order to fill this buffer, we need to do a little maths. We want to play 220 sine waves each second, where each
sine wave completes a full oscillation in 2π "units". This means one seconds worth of audio is 220\*2\*π "units" long.
We then need to split this into 48k samples, basically meaning for each sample we move 220\*2\*π/48k "along" the sine
curve.

```lua {data-peripheral=speaker}
local speaker = peripheral.find("speaker")

local buffer = {}
local t, dt = 0, 2 * math.pi * 220 / 48000
for i = 1, 128 * 1024 do
    buffer[i] = math.floor(math.sin(t) * 127)
    t = (t + dt) % (math.pi * 2)
end

speaker.playAudio(buffer)
```

## Streaming audio
You might notice that the above snippet only generates a short bit of audio - 2.7s seconds to be precise. While we could
try increasing the number of loop iterations, we'll get an error when we try to play it through the speaker: the sound
buffer is too large for it to handle.

Our 2.7 seconds of audio is stored in a table with over 130 _thousand_ elements. If we wanted to play a full minute of
sine waves (and why wouldn't you?), you'd need a table with almost 3 _million_. Suddenly you find these numbers adding
up very quickly, and these tables take up more and more memory.

Instead of building our entire song (well, sine wave) in one go, we can produce it in small batches, each of which get
passed off to @{speaker.playAudio} when the time is right. This allows us to build a _stream_ of audio, where we
read chunks of audio one at a time (either from a file or a tone generator like above), do some optional processing to
each one, and then play them.

Let's adapt our example from above to do that instead.

```lua {data-peripheral=speaker}
local speaker = peripheral.find("speaker")

local t, dt = 0, 2 * math.pi * 220 / 48000
while true do
    local buffer = {}
    for i = 1, 16 * 1024 * 8 do
        buffer[i] = math.floor(math.sin(t) * 127)
        t = (t + dt) % (math.pi * 2)
    end

    while not speaker.playAudio(buffer) do
        os.pullEvent("speaker_audio_empty")
    end
end
```

It looks pretty similar to before, aside from we've wrapped the generation and playing code in a while loop, and added
a rather odd loop with @{speaker.playAudio} and @{os.pullEvent}.

Let's talk about this loop, why do we need to keep calling @{speaker.playAudio}? Remember that what we're trying to do
here is avoid keeping too much audio in memory at once. However, if we're generating audio quicker than the speakers can
play it, we're not helping at all - all this audio is still hanging around waiting to be played!

In order to avoid this, the speaker rejects any new chunks of audio if its backlog is too large. When this happens,
@{speaker.playAudio} returns false. Once enough audio has played, and the backlog has been reduced, a
@{speaker_audio_empty} event is queued, and we can try to play our chunk once more.

## Encoding audio
PCM is a fantastic way of representing audio when we want to manipulate it, but it's not very efficient when we want
to store it to disk. Compare the size of a WAV file (which uses PCM) to an equivalent MP3, it's often 5 times the size.
Instead, we store audio in special formats (or *codecs*) and then convert them to PCM when we need to do processing on
them.

Modern audio codecs use some incredibly impressive techniques to compress the audio as much as possible while preserving
sound quality. However, due to ComputerCraft's limited processing power, it's not really possible to use these from
your computer. Instead, we need something much simpler.

DFPWM (Dynamic Filter Pulse Width Modulation) is the de facto standard audio format of the ComputerCraft (and
OpenComputers) world. Originally popularised by the addon mod [Computronics], CC now has built-in support for it with
the @{cc.audio.dfpwm} module. This allows you to read DFPWM files from disk, decode them to PCM, and then play them
using the speaker.

Let's dive in with an example, and we'll explain things afterwards:

```lua {data-peripheral=speaker}
local dfpwm = require("cc.audio.dfpwm")
local speaker = peripheral.find("speaker")

local decoder = dfpwm.make_decoder()
for chunk in io.lines("data/example.dfpwm", 16 * 1024) do
    local buffer = decoder(chunk)

    while not speaker.playAudio(buffer) do
        os.pullEvent("speaker_audio_empty")
    end
end
```

Once again, we see the @{speaker.playAudio}/@{speaker_audio_empty} loop. However, the rest of the program is a little
different.

First, we require the dfpwm module and call @{cc.audio.dfpwm.make_decoder} to construct a new decoder. This decoder
accepts blocks of DFPWM data and converts it to a list of 8-bit amplitudes, suitable for playing directly with a
speaker.

In order to read the file we use @{io.lines}, consuming the file in chunks of 16KiB. DFPWM uses a single bit for each
sample, which means 16KiB of DFPWM data decodes to 128\*1024 samples, the maximum number a speaker can accept in one go.

[PCM]: https://en.wikipedia.org/wiki/Pulse-code_modulation "Pulse-code Modulation - Wikipedia"
[Sine Wave]: https://en.wikipedia.org/wiki/Sine_wave "Sine wave - Wikipedia"
[Computronics]: https://github.com/Vexatos/Computronics/ "Computronics on GitHub"
