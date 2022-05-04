---
module: [kind=guide] using_modems
see: peripheral The full documentation of the peripheral API
see: rednet The full documentation of the rednet API
---

# Networks and modems
Modems in Computercraft can be used to both transmit messages and connect to peripherals from a distance. We will cover how to do both in this guide.

Modems primary use is to send messages between two or more computers, all three kinds of modem do this slightly differently with the wired modem being the most different. Additionally, there are two APIs for using modems for sending messages, the modem API and the Rednet API. We are only going to cover the rednet API as it is simpler to use and has some additional ease of use features. The modem API is lower level and not as intuitive for just sending messages, if you are really curious about the modem API then you can look up it's documentation and/or check the Rednet API source code for how Rednet uses the lower level API for its needs (FYI, the Rednet API sits on top of the modem API - i.e. it uses the modem API to do its stuff).

## A brief look at the three types of modems
The first modem that we will be looking at is the wired modem. It comes in two forms which both behave extremely similarly, both have the same capabilities aside from two small differences. The original wired modem is the smaller one, it requires networking cable to connect to any other devices as it can only connect to the device that it is attached to (by placing it on the device). Additionally, this smaller wired modem can be a bit limited on what it can be placed on (kind of like torches). The other type of wired modem is a whole block, it doesn't require networking cable in the same way as its smaller counterpart, but can still use those cables. However, unlike its smaller counterpart the full block wired modem will connect to every device that it is in contact with and doesn't have the limitation of placement.

Since we have mentioned them already, networking cables only connect wired modems to other wired modems. You can mix and match the two types of wired modems on the same network. Full block modems can connect to to other full block modems, essentially functioning as a full block network cable.

Additionally, wired networks have a limited distance (256 blocks) that they can go before needing a computer acting as a repeater. Unlike wireless modem range, this cannot be configured.

As for wireless modems, as the name implies they do not use networking cables. They also have the same placement limitations as the small wired modem, but most of the time this is not a problem as wireless modems are usually placed on a computer. Unlike their wired counterpart, the wireless mode cannot be used to connect to peripherals.

The range of wireless modes can be set in ComputerCraft's server side config, so the numbers we will be giving here are just the default values, check the config file or ask your server owner for the actual values. Wireless modems have an interesting property with their range, the higher they are the more range they have. At high altitude the default range is 384 blocks, with it being 64 blocks at low altitude - the range between is calculated by the mod. It used to be that the range would be reduced during thunderstorms, this behavior can be reenabled by setting the appropriate config values. If two modems are talking to each other have different ranges, then the range will be the greatest of the two.

Ender modems are like wireless modems in every way except that their range is functionally infinite and they can send messages across dimensions. Wireless modems and ender modems can send messages to each other, the range of the ender modem is used to determine if the recipient can 'hear' the message. This makes ender modems ideal rednet repeaters.

## Using modems to send messages between computers
No matter which type of modem you use, the process of sending rednet messages is the same. For the rest of this guide we are going to assume that you have two computers, both with compatible modems in an arrangement that they can hear each other. It's recommended that you keep the two computers close to each other as you will be needing to which back and forth between both computers.

Before you begin, you are going to want to note the IDs of both computers that you are using. Rednet uses IDs to specify the *intended* recipient of a message, do note that every computer in range of the message (or connected via wired modems) will receive the message - we will explain this a bit more later. The easiest way to get the ID of a computer is to use the `id` command.

The first thing that every computer needs to do before using rednet is to tell rednet what modem/s it's allowed to use, this is done with `rednet.open(<side>)` where `<side>` is the side of the computer that the modem is on as a string. If your modem is on the top of the computer then you would do `rednet.open("top")`. Opening the same modem twice is not a problem, it will only open it once. Rednet cannot send and receive messages though modems that are not open.

Next you are going to want to set up one of the computers to receive a message. The easiest way to do this is with @{rednet.receive}, this will cause the computer to wait for a rednet message. You could also pull a @{rednet_message} event, but rednet.receive is simpler so we will be using that. While computers can hear every rednet message that they are in range of, the rednet API will ignore those that it's not the intended recipient of.

`rednet.receive()` has three return values but we are only interested in the first two, the id of the sender and the message. You will probably want to print out both of these values. It may also be helpful to put this in a while true loop. Your receiver code might end up looking something like this.

```lua
While true do
  local senderID, message = rednet.receive()
  print("sender ID: "..senderID)
  print("Message: "..message)
  print()
end
```

Then on the other computer we want to use @{rednet.send} to send a message. The first argument is the id of the computer that we want to send the message to, the second argument is the message that we want to send. There is a third argument, but we don't need to use it for this example.

```lua
rednet.send(targetID, "Hello there!")
```

Just make sure to replace the targetID with the ID of the computer that you want to send the message to.

Once both computers have code you'll want to start the receiver code first and then the sender code, this is because you need the receiver to be listening before you have the sender send a message.

You know know the basics of sending and receiving rednet messages. @{rednet.broadcast} is similar to @{rednet.send} but it will send the message to every computer in range of the modem.

:::Note The repeat program
Ender modems are expensive to craft but wireless modems have a quite limited range. Thankfully there is a solution, the repeat program. To put simply, any rednet message that a computer running the repeat program hears will be resent from that computer.

With a single ender modem on a single computer running the repeat program, you can send rednet messages to any computer in the server. Just make sure that this computer and modem stays chunk loaded.
:::

:::Note Computers can send rednet messages to themselves without a modem
Rednet has a loop back feature, computers can send rednet messages to themselves without having a modem attached. This is useful for allowing multiple programs on the same computer to communicate with each other.
:::

:::Note Rednet is not secure
Rednet is not secure. It is not designed to be. Anyone can easily listen to your messages even if they are not the intended recipient. Rednet protocols also are *not* a means of security.

To demonstrate this, here is a very basic rednet sniffer. FYI, we are using the modem API here, specifically its `modem_message` event and `modem.open()`.
```lua
local modem = peripheral.find("modem") or error("No modem attached", 0)
modem.open(rednet.CHANNEL_REPEAT)

while true do
    local event, side, channel, replyChannel, message, distance = os.pullEvent("modem_message");
    print(("Message received on side %s on channel %d (reply to %d) from %f blocks away with message %s"):format(side, channel, replyChannel, distance, tostring(message)))
end
```
To explain what this code is doing, we are exploiting the fact that all rednet messages are sent on two channels, the channel that the intended recipient is listening on and the channel that rednet repeaters listen on `rednet.CHANNEL_REPEAT`.

Additionally, one can use the modem API to impersonate another computer be constructing a fake rednet message.

If you want secure communication then look into encryption, there are a few implementations on the [old] and [new] forums, you may also be able to find some in the creations channel on the [discord].
:::

## Wired modems as peripheral connectors
Something unique to wired modems is there ability to connect peripherals to the computer over a distance. We will not be covering how to use peripherals in this guide, once they are wrapped the process is the same as if they are directly connected to the computer.

There are two significant differences between a peripheral connected via a wired modem and one connected directly to the computer. First, in order to have a peripheral accessible to a computer via a modem you need to right click the modem that is connected to the peripheral, this will add a message in chat with the peripheral's name as well as light up the inner most ring on the modem. The second difference is that instead of using the side that the peripheral is on to @{peripheral.wrap|wrap} the peripheral, you need to give the name of the peripheral that appeared in chat.

Once you have wrapped the remote peripheral you can use it the same as a wrapped peripheral directly connected to the computer.

```lua
local monitor = peripheral.wrap("top")
monitor.write("Hello World!")
```

```lua
local monitor = peripheral.wrap("monitor_1")
monitor.write("Hello World!")
```

## Moving items and fluids
:::Note Fluids are very much the same
We only cover items here, but fluids can be moved in very much the same way.

Some block can have multiple tanks, think of these as multiple inventory slots. The quantity of fluid is like the number of items in the slot.
See the generic peripheral documentation for more info.
:::

There are two key limitations to keep in mind when using computercraft to move items/ First, chests are peripherals and so are bound by the same limitations that peripheral are as described above. Second, the source and destination inventories must be on the same network. Technically both of these are limitations of modems, but the second is more visible when trying to move items as a computer can easily bridge a rednet message to another network with the repeat program.

This means that if you have two chests connected by modems to a single computer you will not be able to move items between the two chests as the modems are their own network. We are using red blocks in the image below to show that the chests cannot move items to each other.

![Two chests connected to a single computer via two separate networks.](images/seperate-modem-networks.png){.big-image}

Since the computer's direct sides count as a separate network, having one chest on a modem and the second directly on the computer also means that items can't move from one to the other.

![Two chests connected to a single computer via two separate networks.](images/seperate-mixed-networks.png){.big-image}

While cables are fine with peripherals, be aware of their length limit as it also applies to peripherals.

Now for a few examples that will work.

Having multiple chests connected though the same modem is fine. Just be aware that you will get one message for each chest in chat with the chest's peripheral name.

![Two chests connected to a single computer via single modem.](images/shared-modem.png){.big-image}

Having to separate modems is fine too. They can be connected via cables too.

![Two chests connected to a single computer via two modems that are one the same network.](images/two-modems-same-network.png){.big-image}

Due to the sides of a computer being the same network, having chests directly connected to the computer allows items to move via the computer instead of via modems. This can be a bit confusing since they can't cross the computer if they are on a modem but it is how it is.

![Two chests connected to a single computer directly.](images/no-modems.png){.big-image}

You can have modems on different sides of the computer as one network if you connect them via cables (or more modems).

![Two chests connected to a single computer on different sides with cables joining them.](images/bridging-cables.png){.big-image}

If your computer is a turtle and you are tying to use the chest peripheral to insert and/or extract items from the turtle then it's a little bit complicated. While turtles do count as inventories, they don't have the generic inventory peripheral, this means that they do not have the push and pull methods that chests and other inventories have. They are still valid targets for those methods though, which we will be look at in a bit.

First though, I do want to mention that while the below can't use the generic inventory methods on the chest peripheral to push items into the turtle, the turtle can still @{turtle.suck|suck} items out of the chest with its turtle API. Since the chest can push items to itself you can use that to rearrange the items in the chest such that the first slot become what you want the turtle to suck as a way to circumvent the turtle's limitation of only being able to suck items from the first non-empty slot.

In the screenshots we are using yellow blocks to show that moving items is possible but requires workarounds.

![A turtle connecting directly to a chest, turtle.suck still works.](images/turtle-direct.png){.big-image}

Now we'll look at a turtle and a chest connected via a modem. Here we can't use the turtle API anymore as the turtle isn't touching the chest, so we have to use the inventory methods. The turtle is a valid target for the chest to push to (and pull from) but we need to know the turtle's network name. This can be done by wrapping the modem and calling it's @{modem.getNameLocal|getNameLocal} method, this gives the turtle's network name which is also the name that the chest will need to be able to push items into the turtle's inventory.

![A turtle connecting to a chest via a modem.](images/turtle-modem.png){.big-image}

[old]: https://www.computercraft.info/forums2/ "The original computercraft forums"

[new]: https://forums.computercraft.cc/index.php "The CC:T forums"

[discord]: https://discord.computercraft.cc/ "The Minecraft Computer Mods Discord"
