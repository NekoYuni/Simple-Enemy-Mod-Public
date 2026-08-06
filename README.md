![image](https://media.forgecdn.net/attachments/description/null/description_00e4c902-0376-49f9-9654-c572b118a7d2.png)


<p align="center">
  <a href="https://modrinth.com/mod/simple-enemy">
    <img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">
  </a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/tacz-simple-enemy">
    <img alt="curseforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg">
  </a>
  <a href="https://github.com/NekoYuni/Simple-Enemy-Mod-Public">
    <img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/available/github_vector.svg">
  </a>
</p>


***

![image](https://media.forgecdn.net/attachments/description/null/description_825149a3-bc69-4274-91f0-252ce64c97aa.png)

**\[SEM\] Simple Enemy Mod** is an Addon that works with TaCZ and adds NPCs to the game equipped with weapons from the latter mod, with the goal of balancing the presence of weapons in the game. Now you will have a reason to carry a weapon.

> **Warning:** Simple Enemy Mod is in early development, which is why it might contain bugs. I highly recommend backing up your worlds to avoid any issues.

***

## **Quick Users Guide**

> Recruiting
> 
> *   Use a _Recruit Table_ to recruit a PMC Unit, or use a spawn egg.
> *   If you use a spawn egg, **Right-Click** the NPC to recruit them.
> 
> Orders
> 
> *   Open the _Commander Menu_ with **Ctrl + C**.
> *   Select the units and send your order.
> 
> Inventory
> 
> *   Press **Shift + Right-Click** on the NPC.

***

![image](https://media.forgecdn.net/attachments/description/null/description_d4b741ad-f194-4766-be08-f870a9795eab.png)

*   **Minecraft Forge:** 47.4.0+
*   **Dependency:** TaCZ 1.1.7
*   **Recommended:** Cloth Config 11.1.136

***

![image](https://media.forgecdn.net/attachments/description/null/description_826bfe63-ca1b-466f-bdca-c494040a00e2.png)

### Factions

3 Factions that will dominate the Minecraft world:

*   Russian Soldiers
*   American Soldiers
*   Recruitable Allied Soldiers

### Advanced Soldier Behavior

*   Long, Mid, and Close-range combat tactics
*   Cover system
*   Flanking system
*   Squad Leader and Member system

### Allies

Soldiers who, for a couple of emeralds, will help you forge the resistance.

*   **Recruit Table:** **Right-click** the block with **16 Emeralds** to recruit an Allied Soldier!
*   **Inventory Management:** **Shift + Right Click** on your ally to edit their customizable inventory.
*   Perfectly integrated for survival mode
*   Customizable inventories
*   Support for most TaCZ weapons
*   Support for Vanilla, Forge, and Gecko 4.4.6 armors

### Commander Menu

Take control of the battlefield. Press **Ctrl + C** to open the menu and give orders to your allies directly:

*   Follow Me
*   Hold Position
*   Free Fire
*   Ceasefire
*   Attack that Target
*   Formation Column
*   Formation Wedge

### Event-based Spawn System

Spawns are based on different dynamic events that occur in-game:

*   Squad patrols
*   Distant firefights for atmosphere
*   Soldier spawns in Villages and Mines

### Commands

*   `/sem event (name of the event) active (True / False)` To Disable or Enable a specific spawnEvent
*   `/sem event (name of the event) spawn` To Force a spawnEvent

***

![image](https://media.forgecdn.net/attachments/description/null/description_9b5cf376-be3e-46cd-aa76-10988d88e169.png)

### Config File

Too easy? Don't worry, you can adjust the soldiers' behavior through the mod's config file! You can edit:

*   Basic entity attributes
*   Engagement range
*   Render distance
*   Soldier accuracy
*   Amount of shots per burst
*   Cooldowns between bursts
*   World spawn probabilities

Find the config file here:  
`.minecraft/saves/[your world name]/serverconfig/sem-server.toml`

***

### Custom Textures

Don't like the soldier skins? That's not a problem! Simple Enemy Mod uses the standard Forge resource path. You can create your own texture pack for the soldiers.

Make sure to follow this folder path in your Zip file: `assets/simpleenemymod/textures/entity/[type of Unit]/texture.png`

**Type of Unit:**

*   `pmc_unit`
*   `ru_unit`
*   `us_unit`

**Texture naming:** `[type of Unit]_variant1.png`

_Examples:_ `pmc_unit_variant3.png` | `ru_unit_variant1.png` | `us_unit_variant2.png`

***

### Custom Weapons

If you want to customize the current weapons of the units, you can use dapatacks.

Official datapack templates can be found on GitHub:  
[https://github.com/NekoYuni/SimpleEnemyMod-Datapack-Templates](https://github.com/NekoYuni/SimpleEnemyMod-Datapack-Templates)

***

![image](https://media.forgecdn.net/attachments/description/null/description_e5d8f580-b1a4-472b-a5f0-2cf42096d134.png)

A special thanks to the **TaCZ team** for providing an API that made it possible to bring Simple Enemy Mod to life.

I also want to give a special thanks to **Default642172**, who authorized me to use their impressive skins in my project.

Finally, I want to thank my small community that has always supported me from the beginning and has been very patient.
