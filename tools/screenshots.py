#!/usr/bin/env python3
"""
Combines screenshots from the Forge and Fabric tests into a single HTML page.
"""
import os
import os.path
from dataclasses import dataclass
from typing import TextIO
from textwrap import dedent
import webbrowser
import argparse

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
                    <span class="desc-prefix">{" » ".join(image.name[:-2])} »</span>
                    <span class="desc-main">{image.name[-1]}</span>
                </span>
            </div>
            """
            )
        )

    io.write("</div></body></html>")


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
        "Forge": "projects/forge",
        "Fabric": "projects/fabric",
    }.items():
        dir = os.path.join(dir, "build", "testScreenshots")
        for file in sorted(os.listdir(dir)):
            name = [project, *os.path.splitext(file)[0].split(".")]
            images.append(Image(name, os.path.join(dir, file)))

    out_file = "build/screenshots.html"
    with open(out_file, encoding="utf-8", mode="w") as out:
        write_images(out, images)

    if args.open:
        webbrowser.open(out_file)


if __name__ == "__main__":
    main()
