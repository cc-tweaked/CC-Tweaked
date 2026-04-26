-- SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local paths = {}

local args = { ... }
for i = 1, #args do
  paths[i] = shell.resolve(args[i])
end

_G.__resolved = paths
