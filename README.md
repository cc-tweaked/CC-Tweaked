# CC: Tweaked build scripts

You probably don't want this branch - check out [the main repository][cct]
instead!

As CC: Tweaked targets multiple Minecraft versions, releasing updates can be a
bit of a pain. This branch provides a script to set up a development environment
and semi-automate releases.

## Usage
```bash
> git clone git@github.com:SquidDev-CC/CC-Tweaked.git
> cd CC-Tweaked
> git checkout build-tools

# Set up the various worktrees.
> ./gfi setup
# Merge branches and publish a release. You probably don't want this unless you
# have all my secret keys.
> ./gfi release
```


[cct]: https://github.com/SquidDev-CC/CC-Tweaked/
