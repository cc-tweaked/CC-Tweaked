---
module: [kind=guide] about_modems
see: peripheral The full documentation of the peripheral API
see: rednet The full documentation of the rednet API
see: modem The full documentation of the modem API
see: basic_rednet For a guide of using rednet to send and receive messages
---

# A look at the different types of modems
<!--TODO: may want to rewrite this-->

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

## Using modems as peripheral connectors
Only wired modems can be used as peripherals connectors.

Place the wired modem on the peripheral block, if it doesn't place then try using a full block modem. Next you'll want to right click the modem to activate it's peripheral mode, if everything worked then you should see a message in the chat window displaying the network name of the peripheral. Make a note of this name, you'll need it to wrap the peripheral. Note that you can click on the message in the chat to copy the peripheral's network name to your clipboard.

Next, connect your computer's modem to the peripheral modem with networking cables (if required). Then you can use the peripheral's network name in place of a side as if you had the peripheral on the computer.

```lua
local monitor = peripheral.wrap("monitor_1")

monitor.write("hello world")
```

[source code]: https://github.com/cc-tweaked/CC-Tweaked/blob/9d50d6414ce84ed2b442c933ca2fb60c97849c6b/src/main/java/dan200/computercraft/shared/peripheral/modem/wireless/WirelessModemPeripheral.java#L40-L57 "Wireless modem range calculation"
