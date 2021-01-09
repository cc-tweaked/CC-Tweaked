# Just my list of things I have ported over

Format for the changelog of ported stuff
```
commit    // Shows commit from CC:T
commit2   // Shows a commit that is the same thing, just a clean up, only if right after
Title     // Commit Title
SubScript // Desc of commit
```

If a edit that is present in CC:T is not needed, I will skip over it.
Any and all references to an issue number, are to be found on CC:T's repo. 

Any commit that starts with `[Patchwork]` are purely edits made by my hand, and not based on other commits from CC:T, this is to help differentiate my changes from the official changes

Lines that are found above a commit in this log like this one, (excluding this one) are comments about how i had to implement things that are not a simple 1:1 (excluding fabric/forge differences) conversion

```md
5155e18de279a193c558aa029963486fd1294769
Added translation for Vietnamese
Co-authored-by: Boom <boom@flyingpackets.net>
```

```
7e121ff72f2b1504cd6af47b57500876682bac45
ae6124d1f477487abab1858abde8c4ec49dfee3c
Translations for Vienamese
Co-authored-by: Boom <boom@flyingpackets.net>
```

```
59de21eae29849988e77fad6bc335f5ce78dfec7
Handle tabs when parsing JSON
Fixes #539
```

```
748ebbe66bf0a4239bde34f557e4b4b75d61d990
Bump to 1.92.0
A tiny release, but there's new features so it's technically a minor
bump.
```

Cherry Picked because this update was partially related to forge updates rather than mod updates
```
8b4a01df27ff7f6fa9ffd9c2188c6e3166edd515
Update to Minecraft 1.16.3

I hope the Fabric folks now realise this is gonna be a race of who can
update first :p. Either way, this was a very easy update - only changes
were due to unrelated Forge changes.
```

```
87393e8aef9ddfaca465d626ee7cff5ff499a7e8
Fix additional `-` in docs

Why isn't this automatically stripped! Bad squid.
```

```
275ca58a82c627128a145a8754cbe32568536bd9
HTTP rules now allow filtering by port

The HTTP filtering system becomes even more complex! Though in this
case, it's pretty minimal, and definitely worth doing.

For instance, the following rule will allow connecting to localhost on
port :8080.

    [[http.rules]]
    host = "127.0.0.1"
    port = 8080
    action = "allow"

    # Other rules as before.

Closes #540
```

The alterations in ColourUtils.java were not needed so they were not ported over
```
6f868849ab2f264508e12c184cc56f2632aaf5bc
Use tags to check if something is a dye

We half did this already, just needed to change a couple of checks.
Closes #541.
```

```
6cee4efcd3610536ee74330cd728f7371011e5a8
Fix incorrect open container check

Was this always broken, or did it happen in a Minecraft update? Don't
know, but it's a very silly mistake either way. Fixes #544
```

```
0832974725b2478c5227b81f82c35bbf03cf6aba
Translations for Swedish

Co-authored-by: David Isaksson <davidisaksson93@gmail.com>
```

```
84036d97d99efd8762e0170002060ae3471508bf
Fix io.open documentation

Well, that was silly.
```

I set the default properties for computers as `Block.GLASS` and then set their strength to `2F` and their soundgroup to stone
```
8472112fc1eaad18ed6ed2c6c62b040fe421e81a
Don't propagate adjacent redstone signals for computers (#549)

Minecraft propagates "strong" redstone signals (such as those directly
from comparators or repeaters) through solid blocks. This includes
computers, which is a little annoying as it means one cannot feed
redstone wire from one side and a repeater from another.

This changes computers to not propagate strong redstone signals, in the
same way transparent blocks like glass do.

Closes #548.
```

```
30d35883b83831900b34040f0131c7e06f5c3e52
Fix my docs

Thanks @plt-hokusai. Kinda embarrassing this slipped through - I
evidently need to lint examples too.
```

```
34a2c835d412c0d9e1fb20a42b7f2cd2738289c7
Add color table to docs (#553)
```

All API Documentation updates, 
`Not Needed` for this repo.
``` 
93068402a2ffec00eedb8fe2d859ebdc005a1989
Document remaining OS functions (#554)

01d81cb91da938836f953b290ad6b8fc87cb7e35
Update illuaminate CSS for deprecation (#556)
```

``` 
Not Needed
4766833cf2d041ed179529eecb9402ad09b2b79b
Bump JEI/crafttweaker versions

In my defence, they weren't out when I started the 1.15 update.
```

``` 
bf6053906dc6a3c7b0d40d5b097e745dce1f33bc
Fix TBO norm issues on old GPUs
```

``` 
Not Needed
113b560a201dbdea9de2a2ef536bcce1d6e51978
Update configuration to match latest illuaminate

Ooooooh, it's all fancy now. Well, that or horrifically broken.
```

```
c334423d42ba3b653ac3a8c27bce7970457f8f96
Add function to get window visibility

Closes #562

Co-authored-by: devomaa <lmao@distruzione.org>
```

[WARN] Could not implement changes to the following files
* `src/main/java/dan200/computercraft/ComputerCraft.java` < Structure too different, cannot find equivalent to alter
* `src/main/java/dan200/computercraft/shared/Config.java` < Files Does not exist in this repo
```
84a6bb1cf3b0668ddc7d8c409a2477a42390e3f7
Make generic peripherals on by default

This is a long way away from "feature complete" as it were. However,
it's definitely at a point where it's suitable for general usage - I'm
happy with the API, and don't think I'm going to be breaking things any
time soon.

That said, things aren't exposed yet for Java-side public consumption. I
was kinda waiting until working on Plethora to actually do that, but not
sure if/when that'll happen.

If someone else wants to work on an integration mod (or just adding
integrations for their own mod), do get in touch and I can work out how
to expose this.

Closes #452
```

``` 
Not Needed
6aae4e576621090840724e094aa25e51696530fc
Remove superfluous imports

Hah, this is embarassing
```

[TODO] [M3R1-01] Code has been applied, players still dont get achievments
``` 
f6160bdc57b3d9850607c2c7c2ce9734b4963478
Fix players not getting advancements when they own turtles

When we construct a new ServerPlayerEntity (and thus TurtlePlayer), we
get the current (global) advancement state and call .setPlayer() on it.

As grantCriterion blocks FakePlayers from getting advancements, this
means a player will no longer receive any advancements, as the "wrong"
player object is being consulted.

As a temporary work around, we attempt to restore the previous player to
the advancement store. I'll try to upstream something into Forge to
resolve this properly.

Fixes #564
```

``` 
17a932920711a5c0361a5048c9e0a5e7a58e6364
Bump cct-javadoc version

Documentation will now be sorted (somewhat) correctly!
```

```
a6fcfb6af2fc1bef8ca3a19122c9267549202424
Draw in-hand pocket computers with blending

It might be worth switching to RenderTypes here, rather than a pure
Tesselator, but this'll do for now.

Fixes Zundrel/cc-tweaked-fabric#20.
```

``` 
c58441b29c3715f092e7f3747bb3ec65ae5a3d29
Various SNBT parsing improvements

Correctly handle:
 - Typed arrays ([I; 1, 2, 3])
 - All suffixed numbers (1.2d)
 - Single-quoted strings

Fixes #559
```

``` 
e2a635b6e5f5942f999213434054e06833c5cb06
Dont fail when codecov is being finicky
```

```
666e83cf4fd0eb327f465d5b919a708790f99b00
Fix JSON objects failing to pass

Maybe I should run the whole test suite, not just the things I think
matter? Nah....
```

``` 
741adfa7bb2b950d2851c3f0072d6a4769f22773

Use blit to draw boxes, add colors.toBlit (#570)
```

``` 
d13bd2cce8d102ad7f61f557e707d6fe3731bc37

use arg[0] in all usage printouts (#571)
```

```
74ac5bb3d17e5bee30643a5d6702696600c06229

Bump to 1.94.0
```

[TODO] [M3R1-02] Zero Clue how to reimplement this in fabric.
``` 
c8aeddedd4ed430f9cb6428676ebb4fa39834182

Auto-generate monitor models

I didn't think it was worth it, and then I found myself needing to
update a dozen of them. The code isn't especially pretty, but it works,
so that's fine.

Also fixes several issues with us using the wrong texture (closes #572).
I've put together a wiki page[1] which describes each texture in a
little more detail.

[1] https://github.com/SquidDev-CC/CC-Tweaked/wiki/Monitor-texture-reference
```

```
7f90f2f7cadce0d5b9177b16626979591bce8137

Clean up some examples a little bit

Would be good if they didn't crash and burn on entry :).
```

```
f194f4fa3a17c48ff1a9088d50063f4a675a23b6

Fix epoch documentation to use milliseconds (#580)
```

```
d2a1a00dc43e5b65f6b64111ce76dd3db16c919f

Clear gets an option to reset the palette (#582)

Fixes #555.
```

```
aab0cd34cd64fdf837ff1c3b91a957a25c2cf7f9

Use term.blit on original paint render

This makes it super speedy, meaning an initial refresh doesn't take ages
to load.
```

```
b0651082f472baee8f0fa8ec7ba95f433e2637bb

Cleanup examples for the various modules
```

Ignored Documentation Changes, these are locate

```
9a749642d294506095e697a3a4345dfe260bd68c

Strict Globals (#583)
```

```
fff8353451451be5ae31e0f63d8e529b127fd186

Remove extra space (#586)
```

```
486f41f08286ddcfad91d72b83a9361bd9c215cb

Fixed length check on function name in `expect` (#589)
```

```
04f9644ae75dafc72da4c4790f334d2e90b03e6f

Allow strings or numbers in textutils.*tabulate

A little dubious, but apparently CC used to support it. This means we're
consistent with methods like io.write or string.len which accept strings
or numbers.

Fixes #591
```

```
d4199064ae5ae8023c589f80f12d94e1c6bbc2b5

Make fs.combine accept multiple arguments

Means we can now do fs.combine("a", "b", "c"). Of course, one may just
write "a/b/c" in this case, but it's definitely useful elsewhere.

This is /technically/ a breaking change as fs.combine(a, b:gsub(...))
will no longer function (as gsub returns multiple arguments). However,
I've done a quick search through GH and my Pastebin archives and can't
find any programs which would break. Fingers crossed.
```

```
24af36743d08fcdb58439c52bf587b33ed828263

Try to handle a turtle being broken while ticked

Hopefully fixes #585. Hopefully.
```

```
511eea39a11956c82e2c11a47b2e7cad27f9887e

Remove <!-- -->s in usages 
```

```
826797cbd579e867f0f35f0be44b6a28c8c094a9

Added documentation for global functions (#592)
```
Didn't port the docs over.

```
d83a68f3ff6e3833278a38798d06215293656e85

Allow $private HTTP rule to block any private IP
```
The config still uses a `blacklist` and `whitelist` array.

```
24d3777722812f975d2bc4594437fbbb0431d910

Added improved help viewer (#595)
```
Didn't port the lua tests over.

```
737b3cb57696fb5517252e7db38bc88ce960b4d8

Don't use capabilities for generic peripherals 
```
Not ported, related to forges capability system which is not used in the port.

```
ea3a16036794357c3a44edffc90fdb652e03881e

Remove a couple of todos
```

```
bb8f4c624bf87169b73fb631d8250cfc38181e15

Some sanity checks for get{Direction,Orientation}
```
Use `getCachedState` instead of forge's `getBlockState` and `contains` instead of `has`.

```
05c3c8ad3269c9025757f9261e7f609889fb6bdc

Generate docs for generic peripherals
```
Skipped everything except some removed whitespace.

```
85cf2d5ff1b63010de4661301801aa504e5b9015

Docs for energy and inventory methods
```
and
```
5865e9c41a0140b9f1acdd2fb095353c467fbb45

Not sure what irritates me more
```
both skipped because the changes where already ported.

```
4ae370b9dbaf1de0ed32a5f32340b1448136c9cc

Merge pull request #606 from TheWireLord/numpadenter-support
```
Just lua changes.

```
f5eb6ce03e0d9bbbf77130452afd4b49e758f7bd

Fix copy-paste error in inventory docs
```
Skipped because it was already ported.

```
663859d2e5a97edefebf9ac36206903d7dd33a3e
Fix double URL decode
```

```
abf425dfb5553483cdc51c50a6b7d8b5e44814f4

Fix overflow in os.epoch
```

```
e3a672099c1b5d2c06f9fe4d8ccd024fef0873a2

Fix JEI integration with turtle/pocket upgrades
```
Skipped because there seems to be no REI integration.

```
2f0cae0bc1b038ac092bafa7f65a317537203cd8

Make upgrade recipe requirements a little more lax
```
[TODO] [JUMT-01] Crafting is still messed up, but this port didn't change the behavior.
[TODO] [JUMT-02] Tag comparison code doesn't need to be that verbose, a simple `isEqual` check would suffice.

```
7f9a707f75636d5816f752dc93d7b6b998c61a03

Bump version to 1.95.0
```
Changed the name from CC: Tweaked to CC: Restitched in the changelog and whatsnew files. New version is 1.95.0-beta.

```
4af5bcc0b0ff464e7e7428c389d47140580ea7a7

Fix serveral 1.15 -> 1.16 issues
```
Skipped, changes where already made.

```
b8d5a89446ac02fc5b38cc5c0b4805de9d11a7d5

Add explicit @module annotation
```
Tiny lua change.

```
8b17ec76a8e94251803e6f4ba4e65970c6a70b7f

Fixed missing argument names in file handle docs (#632)
```
A java doc change.

```
e4b0a5b3ce035eb23feb4191432fc49af5772c5b

2020 -> 2021
```
A huge amount of changes.

```
542b66c79a9b08e080c39c9a73d74ffe71c0106a

Add back command computer block drops
```
Didn't port some forge-related changes, but it works.

```
dd6f97622e6c18ce0d8988da6a5bede45c94ca5d

Prevent reflection errors crashing the game
```

```
92be0126df63927d07fc695945f8b98e328f945a

Fix disk recipes
```
Dye recipes actually work now.

```
1edb7288b974aec3764b0a820edce7e9eee38e66

Merge branch 'mc-1.15.x' into mc-1.16.x
```
New version: 1.95.1.

```
41226371f3b5fd35f48b6d39c2e8e0c277125b21

Add isReadOnly to fs.attributes (#639)
```
Also changed some lua test files, but made the changes anyway.

```
b2e54014869fac4b819b01b6c24e550ca113ce8a

Added Numpad Enter Support in rom lua programs. (#657)
```
Just lua changes.

```
247c05305d106af430fcdaee41371a152bf7c38c

Fix problem with RepeatArgumentType
```

```
c864576619751077a0d8ac1a18123e14b095ec03

Fix impostor recipes for disks
```
[TODO] [JUMT-03] REI still shows white disks, probably because it doesn' show nbt items.