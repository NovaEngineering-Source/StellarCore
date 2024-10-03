# StellarCore Project

This mod requires **MixinBooter 8.0+** and **ConfigAnyTime 2.0+** to work!

### Introduction

StellarCore is a mod that provides extensive bug fixes, performance improvements, and additional features across a wide range of mods, driven by Mixin technology.

Both Server and Client need to install this mod.

There are too many features to list on this page, look for features on the [Sample Configuration](https://github.com/NovaEngineering-Source/StellarCore/blob/master/ExampleConfiguration.cfg) page!

### Compatibility Note

StellarCore is designed to work with other mods, but as a result there are parts that do not work with some mods, the following are parts that may need to be manually configured by the user:

**CensoredASM**
- `resourceLocationCanonicalization` must be turned to false to use StellarCore's `ResourceLocationCanonicalisation` feature

**VintageFix**
- `vintagefix.mixin.dynamic_resources` must be turned to false to use StellarCore's `ParallelModelLoader` feature
- `vintagefix.mixin.dynamic_resources` must be turned to false to use StellarCore's `StitcherCache` feature
- `vintagefix.mixin.textures` must be turned to false to use StellarCore's `ParallelTextureLoad` feature
- `vintagefix.mixin.resourcepacks` must be turned to false to use StellarCore's `ResourceExistStateCache` feature

### How is it different from xxx?

We have more (currently 100+ features, adjustable in the config file).

Like most fix mods, we modify the code to make features work, but our invasiveness is minimal, so you can usually install it alongside other mods without issues.

### Why do most config files lack detailed descriptions?

This mod is still in a relatively early stage of development, but they are already stable!

### Can I add this mod to my ModPack...?

Yes! You can freely add this mod to your ModPack and modify it without restrictions, as long as you are not a bad actor.

### I encountered xxx issue...

Please go to the Source page to submit an issue. We welcome everyone to participate in the development of this mod project!

Join our discord for faster resolve problem: [Cleanroom](https://discord.gg/sgQxDJdrnY")
This mod is fully support CleanroomLoader!
