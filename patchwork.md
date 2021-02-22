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
