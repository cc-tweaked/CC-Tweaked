std = "max"

ignore = {
    -- Allow access to undefined globals or their fields. In the future we'll
    -- define all of CC's globals within this file
    '113', '143',

    -- FIXME: Ignore unused arguments and loop variables
    '212', '213',

    -- Disable line is too long for now. It would be possible to clean
    -- this up in the future.
    '631',
}

-- Only run the linter on ROM and bios for now, as the treasure disks
-- are largely unsupported.
include_files = {
    'src/main/resources/assets/computercraft/lua/rom',
    'src/main/resources/assets/computercraft/lua/bios.lua'
}

files['src/main/resources/assets/computercraft/lua/bios.lua'] = {
    -- Allow declaring and mutating globals
    allow_defined_top = true,
    ignore = { '112', '121', '122', '131', '142' },
}

files['src/main/resources/assets/computercraft/lua/rom/apis'] = {
    -- APIs may define globals on the top level. We'll ignore unused globals,
    -- as obviously they may be used outside that API.
    allow_defined_top = true,
    ignore = { '131' },
}
