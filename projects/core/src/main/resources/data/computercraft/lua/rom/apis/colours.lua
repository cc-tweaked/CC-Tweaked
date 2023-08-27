-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- An alternative version of [`colors`] for lovers of British spelling.
--
-- @see colors
-- @module colours
-- @since 1.2

local colours = _ENV
for k, v in pairs(colors) do
    colours[k] = v
end

--- Grey. Written as `7` in paint files and [`term.blit`], has a default
-- terminal colour of #4C4C4C.
--
-- @see colors.gray
colours.grey = colors.gray
colours.gray = nil --- @local

--- Light grey. Written as `8` in paint files and [`term.blit`], has a
-- default terminal colour of #999999.
--
-- @see colors.lightGray
colours.lightGrey = colors.lightGray
colours.lightGray = nil --- @local
