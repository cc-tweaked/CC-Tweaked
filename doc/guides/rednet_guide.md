---
module: [kind=guide] rednet_guide
see: rednet Send and receive messages over modems.
---

<!--
SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Transferring information wirelessly using Rednet
Being able to send data between computers is an important feature for various 
programs, including turtle controllers and distributed banking systems. The 
[modem](https://tweaked.cc/peripheral/modem.html) peripheral enables computers
and turtles to transmit and receive messages from other computers, and the 
[Rednet](https://tweaked.cc/module/rednet.html) API allows sending messages 
directly to computers by ID, as well as adding hostname lookup and message 
repeating. This guide will show how to use the Rednet API to send and receive 
data, as well as some tips on usage.

## Getting started
To begin using Rednet, you will need at least two computers (or turtles) with 
modems attached. There are three different types of modems available:

- Wireless Modems are the simplest to craft. They have a limited range - 
computers that are too far from each other won't be able to receive each others'
messages. They also don't work across dimensions.
- Ender Modems have infinite range and work across dimensions, but they require
an Eye of Ender to craft. But because they're infinite, messages sent will be 
received by every computer on the server, which could be concerning for 
multiplayer servers where security is required.
- Wired Modems transmit messages through a wire. They don't have limited range, 
but they'll only send messages to computers connected to the same wire. They can
also connect remote peripherals, including inventories like chests. There are 
two variations: the "half width" modems connect to computers directly, while 
"full block" modems can connect to turtles, and can function as wires as well.

To attach a modem to a computer block, simply hold shift and right-click on the
computer. To attach a modem to a turtle or pocket computer, craft the computer
with a wireless modem, or use the `equip` program with the modem's slot
selected. If you use a wired modem, you'll also need to right-click the modem to
turn it red - this will connect the computer to the network.

Before using Rednet, your program needs to open it. Opening Rednet will tell the
modem to start listening for messages from other computers. The
[`rednet.open`](https://tweaked.cc/module/rednet.html#v:open) function takes the
side of the modem to open as a string. On pocket computers, this will always be
`back`. For example, if your modem is on the left side, you would use this code:

```lua
rednet.open("left")
```

Always run this function before you do anything with Rednet - otherwise, it may
not function as expected.

## Sending messages
To send a message to another computer, use the
[`rednet.send`](https://tweaked.cc/module/rednet.html#v:send) function. This
function takes the ID of the computer to send to (which you can get with the
`id` command), the message you want to send, and the protocol to use if required
(more on protocols later).

This line will send the string `"Hello, World!"` to computer ID 3.

```lua
rednet.send(3, "Hello, World!")
```

You can send almost any type of value, including tables. You can also send
variables. Here is an example which sends a table of values from a variable:

```lua
local message = {
    name = "My Message",
    length = 5,
    contents = {1, 2, 5, 4, 3}
}
rednet.send(3, message)
```

You can even send the contents of files using
[`fs.open`](https://tweaked.cc/module/fs.html#v:open):

```lua
local file = assert(fs.open("myfile.txt", "r")) -- Opens the file for reading, with assert to error if it fails.
local data = file.readAll() -- Read the full file into a variable.
file.close() -- Always close files when you're done!
rednet.send(3, data)
```

Note that you cannot send the file handle itself - functions can't be sent over
Rednet, so the file reading functions will get removed from the table, resulting
in sending an empty table.

If a message needs to be sent to every computer available,
[`rednet.broadcast`](https://tweaked.cc/module/rednet.html#v:broadcast) can be
used. It takes the message to send, as well as the protocol if desired.

```lua
rednet.broadcast("Emergency message!")
```

## Receiving messages
Sending messages isn't really useful unless there's a way to receive them. The
[`rednet.receive`](https://tweaked.cc/module/rednet.html#v:receive) function
**waits** for a message to be received, and can wait for a certain amount of
time, or filter for a specific protocol (again, more about that later). It
returns the ID of the computer that sent the message, and then the message (and
finally the protocol if specified), as *multiple return values* (NOT a table).

Here is a simple example that waits for any message without a timeout:

```lua
local id, message = rednet.receive()
```

This example will wait for 5 seconds to receive a message - if nothing is sent
in 5 seconds, it will return `nil`.

```lua
local id, message = rednet.receive(5)
if id == nil then -- Check if it timed out.
    error("No message received!")
end
```

It's a good idea to check that the message came from the right computer before
processing it further. This chunk will check that the message was sent by
computer 1, and if so, will print the message sent to the screen:

```lua
local id, message = rednet.receive()
if id == 1 then -- Check for the right sender.
    print("Message name:", message.name)
end -- Ignore it if another computer sent it.
```

Usually, a server-like program will combine this with an infinite loop to
constantly process requests. Here's a full example of a server with multiple
commands that can be triggered:

```lua
while true do -- Repeat forever.
    local id, message = rednet.receive()
    if id == 1 then -- Check for the right sender.
        -- Check the message command, and execute it.
        if message == "forward" then
            turtle.forward()
        elseif message == "back" then
            turtle.back()
        elseif message == "left" then
            turtle.turnLeft()
        elseif message == "right" then
            turtle.turnRight()
        elseif message == "dig" then
            turtle.dig()
        elseif message == "quit" then
            break -- This will break out of the infinite loop, quitting the program.
        end -- Do nothing on an invalid command.
    end -- Ignore it if another computer sent it.
end
```

## Filtering messages with protocols
Checking for messages by computer ID is a quick way to check that a message is
intended for this server, but sometimes multiple computers need to send messages
to one computer. Protocols allow you to add a special "tag" to a message, which
is used to filter messages. This will prevent computers from accidentally
receiving messages that weren't meant for them.

To add a protocol to a message, simply pass it as the third argument to
`rednet.send`:

```lua
rednet.send(3, message, "myProtocol")
```

To keep your protocol unique to yourself, while also being identifiable, it's
recommended to use a protocol with your name (or a reverse URL) and a program
name separated by a dot:

```lua
rednet.send(3, message, "SquidDev.better-chat")
-- if you own a relevant URL, use it instead:
rednet.send(3, message, "cc.squiddev.better-chat")
```

On the receiving end, messages can be filtered as the last argument to
`rednet.receive`:

```lua
local id, message = rednet.receive("cc.squiddev.better-chat")
-- if you want a timeout, put it first:
local id, message = rednet.receive(5, "cc.squiddev.better-chat")
```

The protocol is returned as an additional third return value, which can be used
to do manual filtering if more than one protocol is necessary:

```lua
local id, message, protocol = rednet.receive()
if protocol == "cc.squiddev.better-chat.message" then
    -- ...
elseif protocol == "cc.squiddev.better-chat.query" then
    -- ...
end
```

## Dynamic host ID lookup
Sometimes, you may not know the exact computer ID to send to - you only know
that you need to send a certain protocol. Rednet features a dynamic host lookup
procedure, which lets servers advertise that they listen to a certain protocol.
Other computers can broadcast a message asking what servers support a protocol,
and any computers which "host" it will respond with their ID. These IDs can be
used directly in `rednet.send` calls. This makes setting up large networks
simpler, as you don't need to hard-code the computer ID into each and every
computer. It also allows creating a list of computers that support a protocol -
for example, creating a server list for a chat program.

To advertise a protocol, use the
[`rednet.host`](https://tweaked.cc/module/rednet.html#v:host) function. This
function takes the protocol to host, as well as a name to call the computer on
the network.

```lua
rednet.host("cc.squiddev.better-chat", "super-cool-computer")
```

Once your program finishes, remember to use the
[`rednet.unhost`](https://tweaked.cc/module/rednet.html#v:unhost) function to
stop advertising the protocol:

```lua
rednet.unhost("cc.squiddev.better-chat")
```

The [`rednet.lookup`](https://tweaked.cc/module/rednet.html#v:lookup) function
takes a protocol to search for, and optionally a name to find, and it returns
*multiple return values* with each computer ID found, or `nil` if there were no
matches. Here's a basic example looking for any computer hosting a protocol:

```lua
local ids = {rednet.lookup("cc.squiddev.better-chat")} -- Wraps the multiple return values into a single table.
```

This will look for a computer with a specific name, erroring if it's not found:

```lua
local id = rednet.lookup("cc.squiddev.better-chat", "super-cool-computer") -- Does not create a table - takes the first return value directly.
if id == nil then error("No route to host") end
```

This example will scan for a protocol, and pings each computer which hosts, but
errors if there are no computers found.

```lua
local ids = {rednet.lookup("cc.squiddev.better-chat")}
if #ids == 0 then -- Checks if the table is empty.
    error("No computers found!")
else
    for _, id in ipairs(ids) do -- Loop over all IDs in the table.
        print("Found computer", id) -- Print the ID to the screen.
        rednet.send(id, "PING!", "cc.squiddev.better-chat") -- Send a message to the computer.
    end
end
```

## Security over Rednet
One important thing to be aware of, especially on multiplayer servers, is that
Rednet is not a secure protocol. Anyone can intercept messages sent over it
(unless the message is sent through a wired modem), with the contents available
in plain text. Furthermore, the ID of a message can be spoofed, and a protocol
doesn't guarantee that the message is in the right format.

If security and authentication are necessary for your application, you should
add encryption to messages. This requires extra code from other people, and is
often more complex than simple send/receive message calls. But for things like
banking and secure chat, encryption is necessary to keep communications safe.

The simplest way to use encryption is through a library like [ECNet2], which
uses protocols to create two-way encrypted pipes (or "sockets") between a client
and server. See the examples there for more info on how to use it.

You can also use encryption primitives directly. There are various algorithms
that each contribute to the security of a message. They have different purposes,
so you'll need to pick and choose which ones apply to your use case.

> [Expert zone][!WARNING]
> 
> These terms may be hard to understand for beginners. Cryptography is a very
> complex field, and uses a lot of terms that may be unfamiliar to novices. This
> guide attempts to boil it down to more understandable terms, but even so, it
> may not be enough for someone new to programming to understand. If in doubt,
> use ECNet, or cross your fingers that nobody will peek at your messages.

- SHA-256 is a hashing algorithm, made popular by its extensive use in Bitcoin.
SHA-256 takes a large string of data, and mashes it up in a semi-random way to
create a 32-byte string/number that represents the data, called a "hash" (or
"digest"). The hash cannot easily be reversed into its original string, but the
same string will create the same hash every time. SHA-256 is useful for
applications where you need to know whether two strings are the same without
storing the actual string, such as checking passwords. SHA-256 is succeeded by
SHA-512, the SHA-3 family of algorithms, and the BLAKE3 family of algorithms,
which are all more secure and have a larger hash than SHA-256; but SHA-256 is
much simpler to implement and faster in CC, as well as still being unbroken, so
it remains the most popular choice for hashing algorithms.
- HMAC is a message authentication algorithm, used for checking the validity and
sender of a message, and is based on a hashing algorithm. HMAC takes a string
message, and a secret key, and creates an "authentication tag" from those two.
The authentication tag is then sent next to the message, but the key is kept
safe and out of the message. On the other end, the receiver can verify the
message by creating its own authentication tag from the message and its copy of
the key, and compares that with the authentication tag with the message. This
ensures that the message wasn't tampered with, and that the message was sent by
the original sender, because changing either the message or key will create a
different tag on the other side. However, it does not hide the contents of the
message. Poly1305 is a more modern and faster alternative that's often used
instead of HMAC, and uses a special algorithm instead of hashing.
- PBKDF2 is a key generation algorithm, which makes a random encryption key from
a password. PBKDF2 takes a password and a random string called a "salt", and
generates a key that can be used for encryption. It uses a large number of
cycles of an algorithm like HMAC to create a key that's random enough to not be
crackable, which also increases the amount of time to try each password. PBKDF2
is useful not only for encryption using a password, but also for checking
passwords, as it makes it even more difficult to reverse the password hash.
- ChaCha20 is a symmetric encryption algorithm, which uses a shared key to hide
the contents of a message. It takes the message, a key, and 12 bytes of random
"salt" data, and creates a scrambled string that can only be decoded with the
same key and salt. The salt should be stored next to the message, but the key
should be kept safe and never sent anywhere. AES is a popular alternative to
ChaCha20, but it's more complex and slower, which makes it a poor fit for
ComputerCraft. ChaCha20 is often paired with Poly1305 to make a type of
encryption known as *Authenticated Encryption with Associated Data* (AEAD),
which both hides a message *and* authenticates it with the original sender, and
it can even authenticate plain text in the same operation.
- Curve25519/X25519 is an elliptic curve (ECC) asymmetric cryptographic
algorithm, which uses separate publicly shareable and private keys to exchange
data in a trusted way. Its most common usage is for Elliptic Curve
Diffie-Hellman (ECDH), which is able to create a shared symmetric key by only
sending the public keys of each computer. This key can then be used for ChaCha20
encryption. X25519 and ECDH are important for encryption because they allow
computers to create a secret key, without sending enough data for other
computers to see the secret as well. To use ECDH, first generate a public and
private keypair on both sides; then send just the *public* key from both
computers to each other. Once both computers have each others' public keys, run
ECDH using *this* computer's *private* key, and the *other* computer's *public*
key. Through the work of magic, both computers will have the same secret key
despite never sharing it directly, and no other computer will be able to create
it because they don't have a private key.
- Ed25519 is a variant of X25519 that allows *signing* messages. Signing is like
message authentication, but uses asymmetric public and private keys to create
the tag. Signing involves using the private key on the message to create a
signature, and verification uses the public key on the signature to check it
with the original message. Signing is used heavily in the real world in HTTPS,
which uses "certificates" signed by higher powers to encrypt the connection. The
signature ensures that the website can be trusted and is who it says it is.
Signing is useful anywhere where authenticating the source and validity of a
message is important, but it's critical that the receiver can't create its own
authentication tags.

Some popular encryption primitive libraries include
[Anavrins's SHA256/HMAC/PBKDF2 library], [PG231's ECC library], and [CCryptoLib]
which is used in ECNet2.

## Conclusion
Remote tasks become a lot easier when computers can communicate together. The
Rednet API makes it possible to send messages between computers simply.
`rednet.send` and `rednet.receive` are the building blocks for transmitting
messages over modems. Protocols help smoothen out these functions by filtering
out the messages that aren't important. Host lookup makes it possible to send to
computers without needing to know their IDs directly. And despite Rednet being
an insecure protocol, there are many ways to fortify your connection against
unwanted spying and manipulation. These tools are fundamental to any program
that relies on remote communication.

[ECNet2]: https://github.com/migeyel/ecnet/
[Anavrins's SHA256/HMAC/PBKDF2 library]: https://pastebin.com/6UV4qfNF
[PG231's ECC library]: https://www.computercraft.info/forums2/index.php?/topic/29803-elliptic-curve-cryptography/
[CCryptoLib]: https://github.com/migeyel/ccryptolib/
