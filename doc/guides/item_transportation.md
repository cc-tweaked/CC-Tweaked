---
module: [kind=guide] item_transportation
see: peripheral The full documentation of the peripheral API
see: inventory The full documentation of generic inventory peripherals
see: about_modems The differences between the types of modems
---

# How to use ComputerCraft to move items around

There are two key limitations to keep in mind when using computercraft to move items. First, chests are peripherals and so are bound by the same limitations that peripherals. Second, the source and destination inventories must be on the same network. Technically both of these are limitations of peripherals and modems, but the second is more visible when trying to move items as a computer can easily bridge a rednet message to another network with the repeat program.

This means that if you have two chests connected by different modems to a single computer you will not be able to move items between the two chests as the modems are their own networks. We are using red blocks in the image below to show that the chests cannot move items to each other.

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

If you are using a turtle and you are trying to use the chest peripheral to insert and/or extract items from the turtle then it's a little bit complicated. While turtles do count as inventories, they don't have the generic inventory peripheral, this means that they do not have the push and pull methods that chests and other inventories have. They are still valid targets for those methods though, which we will be looking at in a bit.

First, I do want to mention that while the below can't use the generic inventory methods on the chest peripheral to push items into the turtle, the turtle can still @{turtle.suck|suck} items out of the chest with its turtle API. Since the chest can push items to itself you can use that to rearrange the items in the chest such that the first slot becomes what you want the turtle to suck as a way to circumvent the turtle's limitation of only being able to suck items from the first non-empty slot.

In the screenshots, we are using yellow blocks to show that moving items is possible but requires workarounds.

![A turtle connecting directly to a chest, turtle.suck still works.](/images/turtle-direct.png){.big-image}

Now we'll look at a turtle and a chest connected via a modem. Here we can't use the turtle API anymore as the turtle isn't touching the chest, so we have to use the inventory methods. The turtle is a valid target for the chest to push to (and pull from) but we need to know the turtle's network name. This can be done by wrapping the modem and calling its @{modem.getNameLocal|getNameLocal} method, this gives the turtle's network name which is also the name that the chest will need to be able to push items into the turtle's inventory.

![A turtle connecting to a chest via a modem.](/images/turtle-modem.png){.big-image}
