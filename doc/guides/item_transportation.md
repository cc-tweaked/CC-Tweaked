---
module: [kind=guide] Item transportation
see: peripheral The full documentation of the peripheral API
see: inventory The full documentation of generic inventory peripherals.
---


Modems in Computercraft can be used to both transmit messages and connect to peripherals from a distance. We will cover how to do both in this guide.

A modem's primary use is to send messages between two or more computers, all three kinds of modem do this slightly differently with the wired modem being the most different. Additionally, there are two APIs for using modems for sending messages, the modem API and the Rednet API. We are only going to cover the rednet API as it is simpler to use and has some additional ease of use features. The modem API is lower level and not as intuitive for just sending messages, if you are curious about the modem API then you can look up its documentation and/or check the Rednet API source code for how Rednet uses the lower-level API for its needs (FYI, the Rednet API sits on top of the modem API - i.e. it uses the modem API to do its stuff).

## A brief look at the three types of modems
The first modem that we will be looking at is the wired modem. It comes in two forms which both behave extremely similarly, both have the same capabilities aside from two small differences. The original wired modem is the smaller one, it requires networking cable to connect to any other devices as it can only connect to the device that it is attached to (by placing it on the device). Additionally, this smaller wired modem can be a bit limited on what it can be placed on (kind of like torches). The other type of wired modem is a whole block, it doesn't require networking cable in the same way as its smaller counterpart but can still use those cables. However, unlike its smaller counterpart, the full block wired modem will connect to every device that it is in contact with and doesn't have the limitation of placement.

Since we have mentioned them already, networking cables only connect wired modems to other wired modems. You can mix and match the two types of wired modems on the same network. Full block modems can connect to other full block modems, essentially functioning as a full block network cable.

Additionally, wired networks have a limited distance (256 blocks) that they can go before needing a computer acting as a repeater. Unlike wireless modem range, this cannot be configured.

As for wireless modems, as the name implies they do not use networking cables. They also have the same placement limitations as the small wired modem, but most of the time this is not a problem as wireless modems are usually placed on a computer. Unlike their wired counterpart, the wireless modem cannot be used to connect to peripherals.

The range of wireless modes can be set in ComputerCraft's server-side config, so the numbers we will be giving here are just the default values, check the config file or ask your server owner for the actual values. Wireless modems have an interesting property with their range, the higher they are the more range they have. At high altitude the default range is 384 blocks, with it being 64 blocks at low altitude - the range between is calculated by the mod. It used to be that the range would be reduced during thunderstorms, this behaviour can be re-enabled by setting the appropriate config values. If two modems talking to each other have different ranges, then the range will be the greatest of the two.

Ender modems are like wireless modems in every way except that their range is functionally infinite and they can send messages across dimensions. Wireless modems and ender modems can send messages to each other, the range of the ender modem is used to determine if the recipient can 'hear' the message. This makes ender modems ideal rednet repeaters.

## Moving items
There are two key limitations to keep in mind when using computercraft to move items. First, chests are peripherals and so are bound by the same limitations that peripherals are as described above. Second, the source and destination inventories must be on the same network. Technically both of these are limitations of modems, but the second is more visible when trying to move items as a computer can easily bridge a rednet message to another network with the repeat program.

This means that if you have two chests connected by modems to a single computer you will not be able to move items between the two chests as the modems are their own networks. We are using red blocks in the image below to show that the chests cannot move items to each other.

![Two chests connected to a single computer via two separate networks.](/images/separate-modem-networks.png){.big-image}

Since the computer's direct sides count as a separate network, having one chest on a modem and the second directly on the computer also means that items can't move from one to the other.

![Two chests connected to a single computer via two separate networks.](/images/separate-mixed-networks.png){.big-image}

While cables are fine with peripherals, be aware of their length limit as it also applies to peripherals.

Now for a few examples that will work.

Having multiple chests connected through the same modem is fine. Just be aware that you will get one message for each chest in the chat with the chest's peripheral name.

![Two chests connected to a single computer via single modem.](/images/shared-modem.png){.big-image}

Having to separate modems is fine too. They can be connected via cables too.

![Two chests connected to a single computer via two modems that are on the same network.](/images/two-modems-same-network.png){.big-image}

Due to the sides of a computer being the same network, having chests directly connected to the computer allows items to move via the computer instead of via modems. This can be a bit confusing since they can't cross the computer if they are on a modem but it is how it is.

![Two chests connected to a single computer directly.](/images/no-modems.png){.big-image}

You can have modems on different sides of the computer as one network if you connect them via cables (or more modems).

![Two chests connected to a single computer on different sides with cables joining them.](/images/bridging-cables.png){.big-image}

If your computer is a turtle and you are trying to use the chest peripheral to insert and/or extract items from the turtle then it's a little bit complicated. While turtles do count as inventories, they don't have the generic inventory peripheral, this means that they do not have the push and pull methods that chests and other inventories have. They are still valid targets for those methods though, which we will be looking at in a bit.

First, though, I do want to mention that while the below can't use the generic inventory methods on the chest peripheral to push items into the turtle, the turtle can still @{turtle.suck|suck} items out of the chest with its turtle API. Since the chest can push items to itself you can use that to rearrange the items in the chest such that the first slot becomes what you want the turtle to suck as a way to circumvent the turtle's limitation of only being able to suck items from the first non-empty slot.

In the screenshots, we are using yellow blocks to show that moving items is possible but requires workarounds.

![A turtle connecting directly to a chest, turtle.suck still works.](/images/turtle-direct.png){.big-image}

Now we'll look at a turtle and a chest connected via a modem. Here we can't use the turtle API anymore as the turtle isn't touching the chest, so we have to use the inventory methods. The turtle is a valid target for the chest to push to (and pull from) but we need to know the turtle's network name. This can be done by wrapping the modem and calling it's @{modem.getNameLocal|getNameLocal} method, this gives the turtle's network name which is also the name that the chest will need to be able to push items into the turtle's inventory.

![A turtle connecting to a chest via a modem.](/images/turtle-modem.png){.big-image}

[old]: https://www.computercraft.info/forums2/ "The original computercraft forums"

[new]: https://forums.computercraft.cc/index.php "The CC:T forums"

[discord]: https://discord.computercraft.cc/ "The Minecraft Computer Mods Discord"
