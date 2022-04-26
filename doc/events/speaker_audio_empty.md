---
module: [kind=event] speaker_audio_empty
see: speaker.playAudio To play audio using the speaker
---

## Return Values
1. @{string}: The event name.
2. @{string}: The name of the speaker which is available to play more audio.


## Example
This uses @{io.lines} to read audio data in blocks of 16KiB from "example_song.dfpwm", and then attempts to play it
using @{speaker.playAudio}. If the speaker's buffer is full, it waits for an event and tries again.

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
