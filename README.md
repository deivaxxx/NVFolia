<img width="1672" height="941" alt="EtheriumMC png" src="https://github.com/user-attachments/assets/ebcbd060-7fd1-474b-84cc-9ea3df71e00d" />


# EtheriumMC 🔮 1.21.5 - 26.2 Spigot Plugins Support
A fork of Folia that supports the Bukkit API and 80% of Spigot/Paper plugins

## I am not an official developer of Folia core. I am just creating a fork of it that will help run 90% of plugins on a multithreaded core.
Please! The EtheriumMC is in effective development, and when using it, save a backup of the world

Most plugins have a multithreaded Folia compatibility setting, I'm building an API that runs entirely on Paper/Spigot but adds all of Folia's functionality
# The core is in Stable 26.2!

# What is included in the core component?
> - All Folia components and chunk division
> -  Full availability of Bukkit/Paper plugins
> - Separation of TPS by dimensions
> - Villagers automatically become dumber if there are more than 45 of them in one chunk (Useful for optimization)
> - Separation of TPS by chunk loading distance
> - Voting for restart
> - Increase in server performance by 145%
> - Items that you lost outside of loading will not despawn
> - Fixed problems with duping rails and threads
> - Experience farms on pig-zombie, now give experience as in previous versions
> - You can adjust the rendering range from 8 to 24 chunks (Do not confuse with the simulation range)
> - Adjustment and disabling of all Folia functions without restarting
> - All new functions have been added to the DivineMC config. Selected for more convenient editing
> - Synchronization of TPS by the connection of the portals of Nether and Overworld (So that farms on withers work adequately)



### 🫒 I also use a simplified version of CanvasMC and the PufferFish API.

```Pufferfish```-Adding DAB system and optimizing asynchronous chunk appearance

```Paper``` - The integral structure of the nucleus

```PurPur``` - For more detailed server settings

```DivineMC``` - Used to spawn mobs asynchronously and speed up single-threaded minecraft connection

```Fish``` - Using patches to improve PWT performance

```LeavesMC``` - Using protocols to integrate mods into the Paper structure


### What does the EtheriumMC multithreaded core do, each dimension has its own thread and chunk too. Now if a player is located and loads 16 chunks, then his TPS will be the same as in these 16 chunks. Other players who load their chunks will have their own TPS
