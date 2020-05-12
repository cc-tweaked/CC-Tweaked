--[[-
Control the current pocket computer, adding or removing upgrades.

This API is only available on pocket computers. As such, you may use its
presence to determine what kind of computer you are using:

```lua
if pocket then
  print("On a pocket computer")
else
  print("On something else")
end
```
]]

--- Search the player's inventory for another upgrade, replacing the existing
-- one with that item if found.
--
-- This inventory search starts from the player's currently selected slot,
-- allowing you to prioritise upgrades.
--
-- @throws If an upgrade cannot be found.
function equipBack() end

--- Remove the pocket computer's current upgrade.
--
-- @throws If this pocket computer does not currently have an upgrade.
function unequipBack() end
