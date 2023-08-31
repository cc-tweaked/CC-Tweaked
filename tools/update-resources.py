#!/usr/bin/env python3

# SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
#
# SPDX-License-Identifier: MPL-2.0

"""
Upgrades textures to newer resource pack formats.

Currently implemented transformations:
 - Split gui/corners_*.png and gui/buttons.png textures into smaller textures.
"""

from PIL import Image
import os
import pathlib
import argparse


def box(x: int, y: int, w: int, h: int):
    return (x, y, x + w, y + h)


def unstitch_buttons(input_file: pathlib.Path):
    """Unstitch the button texture,"""
    buttons = Image.open(input_file)

    output_dir = input_file.parent / "buttons"
    output_dir.mkdir(exist_ok=True)

    buttons.crop(box(1, 1, 12, 12)).save(output_dir / "turned_off.png")
    buttons.crop(box(1, 15, 12, 12)).save(output_dir / "turned_off_hover.png")

    buttons.crop(box(15, 1, 12, 12)).save(output_dir / "turned_on.png")
    buttons.crop(box(15, 15, 12, 12)).save(output_dir / "turned_on_hover.png")

    buttons.crop(box(29, 1, 12, 12)).save(output_dir / "terminate.png")
    buttons.crop(box(29, 15, 12, 12)).save(output_dir / "terminate_hover.png")


def unstitch_corners(input_file: pathlib.Path, family: str):
    """Unstitch the corners texture."""

    input = Image.open(input_file)
    output_dir = input_file.parent

    border = Image.new("RGBA", (36, 36))
    # Corners
    border.paste(input.crop(box(12, 28, 12, 12)), box(00, 00, 12, 12))
    border.paste(input.crop(box(24, 28, 12, 12)), box(24, 00, 12, 12))
    border.paste(input.crop(box(12, 40, 12, 12)), box(00, 24, 12, 12))
    border.paste(input.crop(box(24, 40, 12, 12)), box(24, 24, 12, 12))
    # Horizontal bars
    border.paste(input.crop(box(00, 00, 12, 12)), box(12, 00, 12, 12))
    border.paste(input.crop(box(00, 12, 12, 12)), box(12, 24, 12, 12))
    # Vertical bars
    border.paste(input.crop(box(00, 28, 12, 12)), box(00, 12, 12, 12))
    border.paste(input.crop(box(36, 28, 12, 12)), box(24, 12, 12, 12))

    border.save(output_dir / f"border_{family}.png")

    if family != "command":
        # Fatter bottom pocket computer border.
        pocket_computer = Image.new("RGBA", (36, 20))
        # Middle
        pocket_computer.paste(input.crop(box(00, 56, 12, 20)), box(12, 0, 12, 20))
        # Corners
        pocket_computer.paste(input.crop(box(12, 80, 12, 20)), box(00, 0, 12, 20))
        pocket_computer.paste(input.crop(box(24, 80, 12, 20)), box(24, 0, 12, 20))
        pocket_computer.save(output_dir / f"pocket_bottom_{family}.png")

    if family != "colour":
        # Sidebar
        sidebar = Image.new("RGBA", (17, 14))
        sidebar.paste(input.crop(box(0, 102, 17, 14)), box(0, 0, 17, 14))
        sidebar.save(output_dir / f"sidebar_{family}.png")


def main() -> None:
    spec = argparse.ArgumentParser()
    spec.add_argument("dir", type=pathlib.Path)

    dir: pathlib.Path = spec.parse_args().dir

    texture_path = dir / "assets" / "computercraft" / "textures"

    transformed: list[pathlib.Path] = []

    buttons_path = texture_path / "gui" / "buttons.png"
    if buttons_path.exists():
        unstitch_buttons(buttons_path)
        transformed.append(buttons_path)

    for family in ("normal", "advanced", "command", "colour"):
        path = texture_path / "gui" / f"corners_{family}.png"
        if path.exists():
            unstitch_corners(path, family)
            transformed.append(path)

    if len(transformed) == 0:
        print("No files were transformed")
        return

    print("The following files may be deleted")
    for file in transformed:
        print(f" - {file}")

    if input("Do so now? [y/N]").lower() == "y":
        for file in transformed:
            os.remove(file)


if __name__ == "__main__":
    main()
