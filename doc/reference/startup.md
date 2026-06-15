---
module: [kind=reference] startup
see: guide!startup
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Computer startup
When a computer turns on, it searches for files to run as part of the startup process. This page details this process.

For information about creating a basic startup file, see [`guide!startup`].

 1. `/rom/autorun`: Computers first look in the `/rom/autorun` folder, and run every file in that folder. This folder is
    empty by default, but may be extended by datapacks or other mods. See the [example
    datapack](https://github.com/cc-tweaked/datapack-example) for an example.

 2. If the `shell.allow_disk_startup` [setting][`settings`] is `true`, then connected disk drives are searched for a
    `startup` file, `startup.lua` file, or `startup/` directory. The first disk containing these files will be used for
    startup.

     1. The `startup` (*or* `startup.lua`) file will be run.
     2. All programs under `startup/` will be run.

    The order disks are iterated over is not defined, and so it is recommended to only have one disk containing startup
    files connected to a computer.

 3. If no startup files are found on a disk, and the `shell.allow_startup` [setting][`settings`] is `true`, then the
    root directory is searched for startup files in the same way (`startup` *or* `startup.lua`, then all files in
    `startup/`).

When listing a files from a directory (either `startup/` or `rom/autorun`), the result of [`fs.list`] is used
directly. This will always return files in lexicographical order. This means that `startup/a.lua` will always run before
`startup/b.lua`.
