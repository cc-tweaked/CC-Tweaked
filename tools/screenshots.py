#!/usr/bin/env python3

# SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
#
# SPDX-License-Identifier: MPL-2.0

"""
Combines screenshots from the Forge and Fabric tests into a single HTML page.
"""
import argparse
import pathlib
import webbrowser
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from textwrap import dedent
from typing import TextIO

PROJECT_LOCATIONS = [
    "projects/fabric",
    "projects/forge",
]


@dataclass(frozen=True)
class Image:
    name: list[str]
    path: str


def write_images(io: TextIO, images: list[Image]):
    io.write(
        dedent(
            """\
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>CC:T test screenshots</title>
                <style>
                body {
                    padding: 0;
                    margin: 0;
                    font-family:-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Fira Sans", "Droid Sans", "Helvetica Neue", Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
                }

                .content {
                    margin: 1em;
                    gap: 1em;
                    display: grid;
                    grid-template-columns: repeat(auto-fit,minmax(25em,1fr));
                }

                .image {
                    display: flex;
                    flex-direction: column;
                }

                .desc { text-align: center; }
                .desc-prefix { color: #666; }
                </style>
            </head>
            <body>
            <div class="content">
            """
        )
    )

    for image in images:
        io.write(
            dedent(
                f"""\
            <div class="image">
                <img src="../{image.path}" />
                <span class="desc">
                    <span class="desc-prefix">{" » ".join(image.name[:-1])} »</span>
                    <span class="desc-main">{image.name[-1]}</span>
                </span>
            </div>
            """
            )
        )

    io.write("</div></body></html>")


def _normalise_id(name: str) -> str:
    """Normalise a test ID so it's more readable."""
    return name[0].upper() + name[1:].replace("_", " ")


def _format_timedelta(delta: timedelta) -> str:
    if delta.days > 0:
        return f"{delta.days} days ago"
    elif delta.seconds >= 60 * 60 * 2:
        return f"{delta.seconds // (60 * 60)} hours ago"
    elif delta.seconds >= 60 * 2:
        return f"{delta.seconds // 60} minutes ago"
    else:
        return f"{delta.seconds} seconds ago"


def main():
    spec = argparse.ArgumentParser(
        description="Combines screenshots from the Forge and Fabric tests into a single HTML page."
    )
    spec.add_argument(
        "--open",
        default=False,
        action="store_true",
        help="Open the output file in a web browser.",
    )
    args = spec.parse_args()

    images: list[Image] = []
    for project, dir in {
        "Forge": "projects/forge/build/gametest/runGametestClient",
        "Fabric": "projects/fabric/build/gametest/runGametestClient",
        "Fabric (+Sodium)": "projects/fabric/build/gametest/runGametestClientWithSodium",
        "Fabric (+Iris)": "projects/fabric/build/gametest/runGametestClientWithIris",
    }.items():
        for file in sorted(pathlib.Path(dir).glob("screenshots/*.png")):
            name = [project, *(_normalise_id(x) for x in file.stem.split("."))]

            mtime = datetime.fromtimestamp(file.stat().st_mtime, tz=timezone.utc)
            delta = datetime.now(tz=timezone.utc) - mtime

            print(
                f"""{" » ".join(name[:-1]):>50} » \x1b[1m{name[-1]:25} \x1b[0;33m({_format_timedelta(delta)})\x1b[0m"""
            )

            images.append(Image(name, str(file)))

    out_file = "build/screenshots.html"
    with open(out_file, encoding="utf-8", mode="w") as out:
        write_images(out, images)

    if args.open:
        webbrowser.open(out_file)


if __name__ == "__main__":
    main()
