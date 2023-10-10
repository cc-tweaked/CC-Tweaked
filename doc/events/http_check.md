---
module: [kind=event] http_check
see: http.checkURLAsync To check a URL asynchronously.
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`http_check`] event is fired when a URL check finishes.

This event is normally handled inside [`http.checkURL`], but it can still be seen when using [`http.checkURLAsync`].

## Return Values
1. [`string`]: The event name.
2. [`string`]: The URL requested to be checked.
3. [`boolean`]: Whether the check succeeded.
4. <span class="type">[`string`]|[`nil`]</span>: If the check failed, a reason explaining why the check failed.
