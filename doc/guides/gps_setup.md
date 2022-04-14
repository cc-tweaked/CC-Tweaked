---
module: [kind=guide] gps_setup
---

# Setting up GPS
Some quick definitions for this page.
```
GPS host: a computer running the GPS program in host mode
GPS constellation: a group of GPS hosts
```

Before GPS can be used in a dimention (e.g. the Nether) a GPS host constellation needs to be set up. Traditionally this is done near the max build height for maximum range, but ender modems can beat normal modes in range even from bedrock.

In order to give the best results, a GPS constallation needs atleast four computers. One can theroetically get away with three for most GPS requests, but four changes that to 100% accuracy when the requester is within all of their ranges, provided that they are placed correctly. More than four GPS hosts per constallation is redundent, but it does not harm.

## Building a GPS constellation
![An example GPS constellation.](images/gps-constellation-example.png){.big-image}

Assuming that you are using four computers per costallation, you will want each computer that makes up the constallation to be out of plane with atleast one other computer in the constalation. To put this another way, if you can make a straight wall or floor (diagonal also counts as straight, having a corner or curve doesn't) that touches all four computers then you need to move one of the computers so that it's nolonger in contact with the wall/floor.

Having the GPS constallation computers within a few blocks of each other is what people commonly do but is not required. So long as atleast four GPS hosts can respond to the requester the request will be fulfilled.

GPS only works with wireless modems, ender modems count as wireless. It doesn't matter what side you attach them to, just that each computer in the constellation has one.

:::note Ender modems
You might be aware that ender modems have a very large range, this makes them very usesful for setting up GPS hosts. Infact, if you do this then you will likely only need one GPS constellation for the whole dimention (in vanilla this will mean that you'd need three constellation, one for the Overworld, one for the Nether, and one for the End).

Also, don't worry about an ender modem's ability to work across dimentions, the GPS API is smart enough to ignore constellation that are not in its dimention.
:::

## Configureing the constellation
In order to provide computers (or more commonly, turtles) their location, every GPS host needs to know its own location. GPS host can get this from other constellations, but we are going to input it manually, which means that we are going to make a startup file.

It doesn't matter what kind of startup file you use, so long as it's a valid one; the contents of the file is going to be the same.

Here's a template for the startup file's contents:
```lua
shell.run("gps", "host", x, y, z)
```
Where `x`, `y`, and `z` are the respective coordinates of the CC computer. The easiest way to get the computers coordinates is to look at it and press `F3`, its coordinates can then be found on the right side of the screen as `Targeted Block`.

:::note Why use MC's coordinates?
CC doesn't actually care if you use Minecraft's coordinate system, so long as all of the GPS host with overlapping ranges use the same reference point (requesting computers will get confused if hosts have different reference points). However, using MC's coordinate system does provide a nice standard to adopt server wide. It also is good to use as command computers use it when they cheat to find their position with @{gps.locate}.
:::

:::note Modem messages come from the computer's position not the modem's
Wireless modems transmit from the block that they are attached to *not* the block space that they occupy, the coordinates that you input into your GPS host should be the postion of the computer and not the position of the modem.
:::

## A brief look at how the GPS API works in CC
With the exception of command computers and user code overrides, ComputerCraft actually mimics real life GPS; computers triagulate themselves using signals with known sender positions (which is why we have to tell the GPS hosts where they are). This means that the average CC computer will need a wireless modem and be in range of a GPS constellation.

:::note Double duty
You could also set up one of their GPS hosts (per constellation) to run the rednet `repeat` program via @{multishell} or the @{parallel} API. Do note that if you use the parallel API that the console output of the two programs will merge together, this is harmless but can make it harder to understand the output.

Ender modems are also useful for the `repeat` program, and will mean that you only need one computer running this program.
:::
