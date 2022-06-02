---
module: [kind=guide] gps_setup
---

# Setting up GPS
The @{gps} API allows computers and turtles to find their current position using wireless modems.

In order to use GPS, you'll need to set up multiple GPS hosts. These are computers running the special gps host program, which tell other computers the host's position. Several hosts running together are known as a GPS constellation.

Before GPS can be used in a dimension (e.g. the Nether) a GPS host constellation needs to be set up. Traditionally this is done near the max build height for maximum range, but ender modems easily beats a normal modem's max range at any height.

In order to give the best results, a GPS constellation needs at least four computers. More than four GPS hosts per constellation is redundant, but it does not harm.

## Building a GPS constellation
![An example GPS constellation.](/images/gps-constellation-example.png){.big-image}

Assuming that you are using four computers per constellation, you will want each computer that makes up the constellation to be out of plane with at least one other computer in the constellation. To put this another way, if you can make a straight wall or floor (diagonal also counts as straight, having a corner or curve doesn't) that touches all four computers then you need to move one of the computers so that it's no longer in contact with the wall/floor.

Having the GPS constellation computers within a few blocks of each other is what people commonly do but is not required. So long as at least four GPS hosts can respond to the requester the request will be fulfilled.

GPS only works with wireless modems, ender modems count as wireless. It doesn't matter what side you attach them to, just that each computer in the constellation has one.

:::note Ender modems
You might be aware that ender modems have a very large range, this makes them very useful for setting up GPS hosts. If you do this then you will likely only need one GPS constellation for the whole dimension (in vanilla this will mean that you'd need three constellations, one for the Overworld, one for the Nether, and one for the End).

Also, don't worry about an ender modem's ability to work across dimensions, the GPS API is smart enough to ignore constellations that are not in its dimension.
:::

## Configuring the constellation
To provide computers (or more commonly, turtles) their location, every GPS host needs to know its own location. GPS host can get this from other constellations, but we are going to input it manually, which means that we are going to make a startup file.


Here's a template for the startup file's contents:
```lua
shell.run("gps", "host", x, y, z)
```
Where `x`, `y`, and `z` are the respective coordinates of the CC computer. The easiest way to get the computer's coordinates is to look at it and press `F3`, its coordinates can then be found on the right side of the screen as `Targeted Block`.

:::note Why use MC's coordinates?
CC doesn't care if you use Minecraft's coordinate system, so long as all of the GPS hosts with overlapping ranges use the same reference point (requesting computers will get confused if hosts have different reference points). However, using MC's coordinate system does provide a nice standard to adopt server-wide. It also is consistaint with how command computers get their location, they use MC's command system to get their block which returns that in MC's coordinate system.
:::

:::note Modem messages come from the computer's position, not the modem's
Wireless modems transmit from the block that they are attached to *not* the block space that they occupy, the coordinates that you input into your GPS host should be the position of the computer and not the position of the modem.
:::

## A brief look at how the GPS API works in CC
Except for command computers and user code overrides, ComputerCraft mimics real-life GPS; computers trilaterate themselves using signals with known sender positions (which is why we have to tell the GPS hosts where they are). This means that the average CC computer will need a wireless modem and be in range of a GPS constellation.
