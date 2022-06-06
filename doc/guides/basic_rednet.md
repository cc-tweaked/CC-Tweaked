---
module: [kind=guide] The basics of rednet messaging
see: peripheral The full documentation of the peripheral API
see: rednet The full documentation of the rednet API
---

A modem's primary use is to send messages between two or more computers, all three kinds of modem do this slightly differently with the wired modem being the most different. Additionally, there are two APIs for using modems for sending messages, the modem API and the Rednet API. We are only going to cover the rednet API as it is simpler to use and has some additional ease of use features. The modem API is lower level and not as intuitive for just sending messages, if you are curious about the modem API then you can look up its documentation and/or check the Rednet API source code for how Rednet uses the lower-level API for its needs (FYI, the Rednet API sits on top of the modem API - i.e. it uses the modem API to do its stuff).

The main traits and differences of modems that you may need to be aware of are:
* The two types of wired modem connect to things a bit differently, the smaller one can only connect to what it's placed on, the larger one can connect to everything that it's in contact with.
  * Also, the smaller wired modem cannot be placed on everything. Kind of like torches, it needs to be placed on a solid block. This is more relevant when using wired modems to connect to peripherals.
* While both wired modems can use networking cables, the smaller wired modem needs a cable placed in its block space to become functional.
* Full block modems will allow the network to pass through them, effectively allowing them to double as networking cables.
  * If you are running some cables though a wall then having a full block modem as part of the wall can be a nice aesthetic choice.
  * This works even i the modem is only connected to networking cables.
* Wired modems and network cables have a max length of 256 blocks, after which connections will be ignored.
  * This means that if another computer is further away then rednet messages will not reach it and it will not show up @{computer|as a peripheral}.
  * This range cannot be changed in the config.
* Wired modems and wireless modems cannot talk to each other directly. A computer will need to relay messages heard on one modem to the other, there is a built in program that will do this for you it is called `repeat`.
  * Wireless modems and ender modems can talk to each other though.
  * The two types of wired modem also can talk to each other, provided that they are connected to each other.
* Wireless modems have a limited range, ender modems have a practically infinite range.
* By default, wireless modems have their range increase the higher they are in the world. The exact values can be set in the server side config file.
  * The server side config file also allows for having wireless modem range reduce during thunderstorms, but this is not the default.
  * The default range pre 1.18 near bedrock is 64 blocks and 384 blocks near the max build height. The max ranges after 1.18 are likely different now that Minecraft has negative y levels and a higher build limit. You can run the calculation yourself by checking the [source code].
* Ender modems are the only modem that can send and receive messages from other dimensions.

<!--TODO: rewrite-->
## Using modems to send messages between computers
No matter which type of modem you use, the process of sending rednet messages is the same. For the rest of this guide, we are going to assume that you have two computers, both with compatible modems in an arrangement that they can hear each other. It's recommended that you keep the two computers close to each other as you will be needing to which back and forth between both computers.

Before you begin, you are going to want to note the IDs of both computers that you are using. Rednet uses IDs to specify the *intended* recipient of a message, do note that every computer in range of the message (or connected via wired modems) will receive the message - we will explain this a bit more later. The easiest way to get the ID of a computer is to use the `id` command.

The first thing that every computer needs to do before using rednet is to tell rednet what modem/s it's allowed to use, this is done with `rednet.open(<side>)` where `<side>` is the side of the computer that the modem is on as a string. If your modem is on the top of the computer then you would do `rednet.open("top")`. Opening the same modem twice is not a problem, it will only open it once. Rednet cannot send and receive messages through modems that are not open.

Next, you are going to want to set up one of the computers to receive a message. The easiest way to do this is with @{rednet.receive}, this will cause the computer to wait for a rednet message. You could also pull a @{rednet_message} event, but rednet.receive is simpler so we will be using that. While computers can hear every rednet message that they are in range of, the rednet API will ignore those that it's not the intended recipient of.

`rednet.receive()` has three return values but we are only interested in the first two, the id of the sender and the message. You will probably want to print out both of these values. It may also be helpful to put this in a while true loop. Your receiver code might end up looking something like this.

```lua
while true do
  local senderID, message = rednet.receive()
  print("sender ID: "..senderID)
  print("Message: "..message)
  print()
end
```

Then on the other computer, we want to use @{rednet.send} to send a message. The first argument is the id of the computer that we want to send the message to, the second argument is the message that we want to send. There is a third argument, but we don't need to use it for this example.

```lua
rednet.send(targetID, "Hello there!")
```

Just make sure to replace the targetID with the ID of the computer that you want to send the message to.

Once both computers have code you'll want to start the receiver code first and then the sender code, this is because you need the receiver to be listening before you have the sender send a message.

You know the basics of sending and receiving rednet messages. @{rednet.broadcast} is similar to @{rednet.send} but it will send the message to every computer in range of the modem.

:::note The repeat program
Ender modems are expensive to craft but wireless modems have a quite limited range. Thankfully there is a solution, the repeat program. To put simply, any rednet message that a computer running the repeat program hears will be resent from that computer.

With a single ender modem on a single computer running the repeat program, you can send rednet messages to any computer in the server. Just make sure that this computer and modem stay chunk loaded.
:::

:::note Computers can send rednet messages to themselves without a modem
Rednet has a loopback feature, computers can send rednet messages to themselves without having a modem attached. This is useful for allowing multiple programs on the same computer to communicate with each other.
:::

:::note Rednet is not secure
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

Additionally, one can use the modem API to impersonate another computer by constructing a fake rednet message.

If you want secure communication then look into encryption, there are a few implementations on the [old] and [new] forums, you may also be able to find some in the creations channel on the [discord].
:::

[old]: https://www.computercraft.info/forums2/ "The original computercraft forums"

[new]: https://forums.computercraft.cc/index.php "The CC:T forums"

[discord]: https://discord.computercraft.cc/ "The Minecraft Computer Mods Discord"

[source code]: https://github.com/cc-tweaked/CC-Tweaked/blob/9d50d6414ce84ed2b442c933ca2fb60c97849c6b/src/main/java/dan200/computercraft/shared/peripheral/modem/wireless/WirelessModemPeripheral.java#L40-L57 "Wireless modem range calculation"
