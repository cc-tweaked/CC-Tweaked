---
module: [kind=event] speaker_audio_empty
see: speaker.playAudio To play audio using the speaker
---

## Return Values
1. @{string}: The event name.
2. @{string}: The name of the speaker which is available to play more audio.


## Example
This uses @{io.lines} to read audio data in blocks of 16KiB from "example_song.dfpwm", and then attempts to play it
using @{speaker.playAudio}. If the speaker's buffer is full, it waits for an event and tries gain.

```lua
local speaker = peripheral.find("speaker")

for input in io.lines("example_song.dfpwm", 16 * 1024) do
    local data = handle.read(16 * 1024)
    if not data then break end -- We've reached the end of file

    while not speaker.playAudio(data) do
        os.pullEvent("speaker_audio_empty")
    end
end
```
