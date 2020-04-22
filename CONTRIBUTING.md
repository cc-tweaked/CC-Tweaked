# Contributing to CC: Tweaked
As with many open source projects, CC: Tweaked thrives on contributions from other people! This document (hopefully)
provides an introduction as to how to get started in helping out.

If you've any other questions, [just ask the community][community] or [open an issue][new-issue].

## Reporting issues
If you have a bug, suggestion, or other feedback, the best thing to do is [file an issue][new-issue]. When doing so,
do use the issue templates - they provide a useful hint on what information to provide.

## Developing
In order to develop CC: Tweaked, you'll need to download the source code and then run it. This is a pretty simple
process.

 - **Clone the repository:** `git clone https://github.com/SquidDev-CC/CC-Tweaked.git && cd CC-Tweaked`
 - **Setup Forge:** `./gradlew build`
 - **Run Minecraft:** `./gradlew runClient` (or run the `GradleStart` class from your IDE).

If you want to run CC:T in a normal Minecraft instance, run `./gradlew build` and copy the `.jar` from `build/libs`.
These commands may take a few minutes to run the first time, as the environment is set up, but should be much faster
afterwards.

### Code linters
CC: Tweaked uses a couple of "linters" on its source code, to enforce a consistent style across the project. While these
are run whenever you submit a PR, it's often useful to run this before committing.

 - **[Checkstyle]:** Checks Java code to ensure it is consistently formatted. This can be run with `./gradlew build` or
   `./gradle check`.
 - **[illuaminate]:** Checks Lua code for semantic and styleistic issues. See [the usage section][illuaminate-usage] for
   how to download and run it.

[new-issue]: https://github.com/SquidDev-CC/CC-Tweaked/issues/new/choose "Create a new issue"
[community]: README.md#Community "Get in touch with the community."
[checkstyle]: https://checkstyle.org/
[illuaminate]: https://github.com/SquidDev/illuaminate/
[illuaminate-usage]: https://github.com/SquidDev/illuaminate/blob/master/README.md#usage
