#!/usr/bin/env python3
"""
Rewrites language files in order to be consistent with en_us.

This will take every given language file and rewrite it to be in the same
order as the en_us file. Any keys which appear in the given file, but not
in en_us are removed.

Note, this is not intended to be a fool-proof tool, rather a quick way to
ensure language files are mostly correct.
"""

import pathlib, sys, json
from collections import OrderedDict

root = pathlib.Path("src/main/resources/assets/computercraft/lang")

with (root / "en_us.json").open(encoding="utf-8") as file:
    en_us = json.load(file, object_hook=OrderedDict)

for path in root.glob("*.json"):
    if path.name == "en_us.json":
        continue

    with path.open(encoding="utf-8") as file:
        lang = json.load(file)

    out = OrderedDict()
    missing = 0
    for k in en_us.keys():
        if k not in lang:
            missing += 1
        elif lang[k] == "":
            print("{} has empty translation for {}".format(path.name, k))
        else:
            out[k] = lang[k]

    with path.open("w", encoding="utf-8", newline="\n") as file:
        json.dump(out, file, indent=4, ensure_ascii=False)
        file.write("\n")

    if missing > 0:
        print("{} has {} missing translations.".format(path.name, missing))
