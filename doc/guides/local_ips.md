---
module: [kind=guide] local_ips
---

<!--
SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Allowing access to local IPs
By default, ComputerCraft blocks access to local IP addresses for security. This means you can't normally access any
HTTP server running on your computer. However, this may be useful for testing programs without having a remote
server. You can unblock these IPs in the ComputerCraft config.

 - [Minecraft 1.13 and later, CC:T 1.87.0 and later](#cc-1.87.0)
 - [Minecraft 1.13 and later, CC:T 1.86.2 and earlier](#cc-1.86.2)
 - [Minecraft 1.12.2 and earlier](#mc-1.12)

## Minecraft 1.13 and later, CC:T 1.87.0 and later {#cc-1.87.0}
The configuration file can be located at `serverconfig/computercraft-server.toml` inside the world folder on either
single-player or multiplayer. Look for lines that look like this:

```toml
#A list of rules which control behaviour of the "http" API for specific domains or IPs.
#Each rule is an item with a 'host' to match against, and a series of properties. The host may be a domain name ("pastebin.com"),
#wildcard ("*.pastebin.com") or CIDR notation ("127.0.0.0/8"). If no rules, the domain is blocked.
[[http.rules]]
    host = "$private"
    action = "deny"
```

On 1.95.0 and later, this will be a single entry with `host = "$private"`. On earlier versions, this will be a number of
`[[http.rules]]` with various IP addresses. You will want to remove all of the `[[http.rules]]` entries that have
`action = "deny"`. Then save the file and relaunch Minecraft (Server).

Here's what it should look like after removing:

```toml
#A list of rules which control behaviour of the "http" API for specific domains or IPs.
#Each rule is an item with a 'host' to match against, and a series of properties. The host may be a domain name ("pastebin.com"),
#wildcard ("*.pastebin.com") or CIDR notation ("127.0.0.0/8"). If no rules, the domain is blocked.
[[http.rules]]
    #The maximum size (in bytes) that a computer can send or receive in one websocket packet.
    max_websocket_message = 131072
    host = "*"
    #The maximum size (in bytes) that a computer can upload in a single request. This includes headers and POST text.
    max_upload = 4194304
    action = "allow"
    #The maximum size (in bytes) that a computer can download in a single request. Note that responses may receive more data than allowed, but this data will not be returned to the client.
    max_download = 16777216
    #The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited.
    timeout = 30000
```

## Minecraft 1.13 and later, CC:T 1.86.2 and earlier {#cc-1.86.2}
The configuration file for singleplayer is at `.minecraft/config/computercraft-common.toml`. Look for lines that look
like this:

```toml
#A list of wildcards for domains or IP ranges that cannot be accessed through the "http" API on Computers.
#If this is empty then all whitelisted domains will be accessible. Example: "*.github.com" will block access to all subdomains of github.com.
#You can use domain names ("pastebin.com"), wildcards ("*.pastebin.com") or CIDR notation ("127.0.0.0/8").
blacklist = ["127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "fd00::/8"]
```

Remove everything inside the array, leaving the last line as `blacklist = []`. Then save the file and relaunch Minecraft.

Here's what it should look like after removing:

```toml
#A list of wildcards for domains or IP ranges that cannot be accessed through the "http" API on Computers.
#If this is empty then all whitelisted domains will be accessible. Example: "*.github.com" will block access to all subdomains of github.com.
#You can use domain names ("pastebin.com"), wildcards ("*.pastebin.com") or CIDR notation ("127.0.0.0/8").
blacklist = []
```

## Minecraft 1.12.2 and earlier {#mc-1.12}
On singleplayer, the configuration file is located at `.minecraft\config\ComputerCraft.cfg`. On multiplayer, the
configuration file is located at `<server folder>\config\ComputerCraft.cfg`. Look for lines that look like this:

```ini
# A list of wildcards for domains or IP ranges that cannot be accessed through the "http" API on Computers.
# If this is empty then all explicitly allowed domains will be accessible. Example: "*.github.com" will block access to all subdomains of github.com.
# You can use domain names ("pastebin.com"), wildcards ("*.pastebin.com") or CIDR notation ("127.0.0.0/8").
S:blocked_domains <
    127.0.0.0/8
    10.0.0.0/8
    172.16.0.0/12
    192.168.0.0/16
    fd00::/8
 >
```

Delete everything between the `<>`, leaving the last line as `S:blocked_domains = <>`. Then save the file and relaunch
Minecraft (Server).

Here's what it should look like after removing:

```ini
# A list of wildcards for domains or IP ranges that cannot be accessed through the "http" API on Computers.
# If this is empty then all explicitly allowed domains will be accessible. Example: "*.github.com" will block access to all subdomains of github.com.
# You can use domain names ("pastebin.com"), wildcards ("*.pastebin.com") or CIDR notation ("127.0.0.0/8").
S:blocked_domains <>
```
