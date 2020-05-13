import pathlib, sys

problems = False

# Skip images and files without extensions
exclude = [ "*.png", "**/data/json-parsing/*.json" ]

for path in pathlib.Path("src").glob("**/*"):
    # Ideally we'd use generated as a glob, but .match("generated/**/*.json") doesn't work!
    if path.is_dir() or path.suffix == "" or any(path.match(x) for x in exclude) or path.parts[1] == "generated":
        continue

    with path.open(encoding="utf-8") as file:
        has_dos, has_trailing, first, count = False, False, 0, True
        for i, line in enumerate(file):
            if first:
                first = False
                if line.strip() == "":
                    print("%s has empty first line" % path)

            if len(line) >= 2 and line[-2] == "\r" and line[-1] == "\n" and not has_line:
                print("%s has contains '\\r\\n' on line %d" % (path, i + 1))
                problems = has_dos = True

            if len(line) >= 2 and line[-2] in " \t" and line[-1] == "\n" and not has_trailing:
                print("%s has trailing whitespace on line %d" % (path, i + 1))
                problems = has_trailing = True

            if len(line) == 0 or line[-1] != "\n":
                count = 0
            elif line.strip() == "":
                count += 1
            else:
                count = 1

        if count != 1:
            print("%s should have 1 trailing lines, but has %d" % (path, count))
            problems = True

if problems:
    sys.exit(1)
