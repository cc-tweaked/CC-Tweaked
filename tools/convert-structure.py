#!/usr/bin/env python3
import sys
import nbtlib
from nbtlib.tag import Compound, Int, List, String

_, file = sys.argv

with open(file, "r") as h:
    structure = nbtlib.parse_nbt(h.read())



def make_state(item: dict) -> str:
    name = item["Name"]
    if "Properties" in item:
        name += "{" + ",".join(f"{k}:{v}" for k, v in item["Properties"].items()) + "}"
    return String(name)

def make_block(block: dict, new_palette: list) -> dict:
    res = {
        "pos": block["pos"],
        "state": new_palette[block["state"]],
    }
    if "nbt" in block:
        res["nbt"] = block["nbt"]
    return Compound(res)


if __name__ == '__main__':
    new_palette = [make_state(x) for x in structure['palette']]
    new_result = Compound({
        "DataVersion": Int(2730),
        "size": structure["size"],
        "entities": structure["entities"],
        "data": List([make_block(x, new_palette) for x in structure["blocks"]]),
        "palette": List(new_palette),
    })

    with open(file, "w") as h:
        h.write(nbtlib.serialize_tag(new_result, indent=4))
