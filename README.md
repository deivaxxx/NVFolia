<img width="1672" height="941" alt="EtheriumMC png" src="https://github.com/user-attachments/assets/ebcbd060-7fd1-474b-84cc-9ea3df71e00d" />


# EtheriumMC 🔮 1.21.5 - 26.2 Spigot Plugins Support
A fork of Folia that supports the Bukkit API and 80% of Spigot/Paper plugins

## I am not an official developer of Folia core. I am just creating a fork of it that will help run 90% of plugins on a multithreaded core.
Please! The EtheriumMC is in effective development, and when using it, save a backup of the world

Most plugins have a multithreaded Folia compatibility setting, I'm building an API that runs entirely on Paper/Spigot but adds all of Folia's functionality
# The core is in Stable 26.2!

# ⚙️ Features
- **Full availability of Bukkit/Paper plugins**
- **Parallel World Ticking** - Leverage multiple CPU cores for world processing 
- **Async Operations** - Pathfinding, entity tracker, mob spawning, joining, chunk sending and portal pre-loading
- **Regionized Chunk Ticking** - Tick chunks in parallel, this feature is provided to us by the Folia core
- **Linear Region File Format** - Optimize your world with the old V1/V2 linear format and the new Buffered format
- **Mod Protocols Support** - Compatible with Syncmatica, Apple Skin, and others
- **Raytrace Entity Culling** - Stops sending entities a player provably cannot see, saving bandwidth and blinding entity-ESP cheats
- **Parallel Sensor Phase** - Run the expensive read-only part of mob AI (entity scans, line-of-sight checks) on a thread pool
- **Automatic lobotomization Villagers** - If residents exceed more than 45 per chunk, they will automatically begin to become dumber
- **Increased performance and lag compensation** - If the server has less than 10 TPS, then breaking blocks, picking up items, and entering the portal are accelerated and there is no difference from 20 TPS

# [EtheriumMC Vanilla Minecraft Server](https://discord.gg/sakkp6g2Us)

### 🫒 I also use a simplified version of based Purpur.

```Pufferfish```-Adding DAB system and optimizing asynchronous chunk appearance

```Paper``` - The integral structure of the nucleus

```PurPur``` - For more detailed server settings

```DivineMC``` - Used to spawn mobs asynchronously and speed up single-threaded minecraft connection

```Fish``` - Using patches to improve PWT performance

```LeavesMC``` - Using protocols to integrate mods into the Paper structure


### What does the EtheriumMC multithreaded core do, each dimension has its own thread and chunk too. Now if a player is located and loads 16 chunks, then his TPS will be the same as in these 16 chunks. Other players who load their chunks will have their own TPS
