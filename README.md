# SimpleStats ![Gradle CI](https://github.com/mew/SimpleStats/workflows/Gradle%20CI/badge.svg) ![GitHub total downloads](https://img.shields.io/github/downloads/mew/simplestats/total) 
A no-bullshit in-game stats viewer.

Replaces the default `/stats` command with a much more informative text-based version:  
![Stats Image 1](https://i.imgur.com/8znoeTO.png)  
You can also get stats for a specific game (`/stats [player] [game]`):  
![Stats Image 2](https://i.imgur.com/6mY49nO.png)  
As of 1.1, you can get stats for a player's guild too (`/stats [player] guild`):  
![Stats Image 3](https://i.imgur.com/1h2w3rA.png)  
As of 1.3, you can get stats for everyone in your lobby (`/stats * [game]`):
![Stats Image 4](https://i.imgur.com/foybXXy.png)

## Shortcuts
- Use `:` before a name to get stats compactly in one line. (`/stats :[player] <game/guild>`)
- Use `.` instead of a name to check your own stats easily. (`/stats . <game/guild>`)
- Use `*` instead of a name to enter server mode and get stats of everyone on the current server upto 24 players. (`/stats * <game/guild>`)
- Use `#` to get the statistics of your API key. (`/stats #`)

## Installation
1. Download the [latest release](https://github.com/mew/SimpleStats/releases/latest/).
2. Put the JAR into your `mods` folder.
3. When in game, set your Hypixel API key with `/setkey <key>`. Hypixel API key can be acquired by doing `/api new`.
