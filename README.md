> **Disclaimer:**  
> This is an unofficial addon for Meteor Client, made for educational and personal use only.  
> It is not affiliated with Mojang, Microsoft, or Meteor Development. Use at your own risk.
# EyeOfGod Addon for Meteor Client

EyeOfGod is a Meteor Client addon that provides player spying functionality through a live map API.  
It contains two main components:

- **EyeOfGodModule** – core logic for tracking players.
- **EyeOfGodHud** – HUD element for rendering tracked players' nicknames and coordinates on the screen.

---

## Features
- Track players by nickname.
- Detect when tracked players join, leave, or are not found.
- Display tracked players with dimension and coordinates.
- HUD customization: alignment, scaling, background, text options.

---

## Dependencies
- **Minecraft**: 1.20.6+
- **Meteor Client**: *
- **Java**: 21

Make sure these versions are installed before building or running the addon.

---

## Modules

### EyeOfGodModule
Handles player tracking and state management.  
Periodically fetches data from the live map API and updates online/offline/not-found status.

### EyeOfGodHud
Renders tracked players in-game.  
Shows nickname, dimension, and coordinates.  
Works together with EyeOfGodModule.

---

## Usage
1. Install Meteor Client.
2. Place the compiled addon `.jar` into `.minecraft/mods`.
3. Launch the game and open Meteor GUI (`Right Shift`).
4. Enable **EyeOfGodModule**.
5. Configure tracked nicknames in the module settings.
6. (Optional) Enable **EyeOfGodHud** to display player data.
---
in waiting zlib on zlp <3
