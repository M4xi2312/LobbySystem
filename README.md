# MLobbySystem

[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE.txt)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://adoptium.net/)
[![Paper API](https://img.shields.io/badge/Paper-1.21.11%2B-2c2c2c.svg)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-supported-brightgreen.svg)](https://github.com/PaperMC/Folia)
[![GitHub last commit](https://img.shields.io/github/last-commit/M4xi2312/LobbySystem)](https://github.com/M4xi2312/LobbySystem/commits/master)
[![GitHub issues](https://img.shields.io/github/issues/M4xi2312/LobbySystem)](https://github.com/M4xi2312/LobbySystem/issues)

A modern, lightweight lobby system for Paper servers — built with performance and flexibility in mind.
Designed for current server setups, including full support for Folia's regionized multithreading model.

## ✨ Features

* ⚡ **Folia Support** – Built to run safely and efficiently on Folia servers
* 🧭 **Custom Spawn System** – Define and manage your lobby spawn with ease
* 🎮 **Join Management** – Custom or silenced join/quit messages, teleport-on-join/respawn/void
* 🧰 **Modular Hotbar Items** – Add, remove, or reskin as many hotbar items as you want, each with
  its own action: show links, open the server selector, toggle player visibility, run a command,
  open the cosmetics menu, or send a rich clickable chat message
* 💬 **Clickable Chat Messages** – The `show-text` item action sends multi-line MiniMessage text with
  per-line click behavior (open URL, run command, suggest command, copy to clipboard) and a
  `{player}` placeholder
* 🎩 **Cosmetics** – Hats, particle trails, and gadgets (firework burst, grappling hook, pearl bow),
  each optionally permission-gated and persisted per player across restarts
* 🖥️ **Server Selector Menu** – Paginated, MiniMessage-styled proxy server selector
* 🚫 **Lobby Protection** – Disable damage, hunger, block breaking, and more
* 🔄 **Auto Teleport** – Send players to spawn automatically on join, respawn, or void fall
* 🔌 **Scriptable** – Every hotbar item use fires a cancellable `LobbyItemUseEvent`, so other plugins
  or scripts (e.g. Skript) can react to or override item behavior
* 🎛️ **Highly Configurable** – Simple config structure for fast setup and customization

## 🧩 Compatibility

* ✅ Paper 1.21.11, 26.1.x, 26.2 (Java 21+ for the plugin; Paper itself needs Java 25 to run its 26.x builds)
* ✅ Folia (same version range) - built against the shared Paper/Folia API, Folia builds exist for the
  same versions

The plugin is built once against the oldest supported API and verified by booting the same jar against
real Paper 1.21.11, 26.1.2, and 26.2 servers - no separate builds per Minecraft version needed.

## 🚀 Why MLobbySystem?

MLobbySystem focuses on **modern server architecture**: commands are registered through Paper's Brigadier
command API (real tab-completion, no legacy `CommandExecutor`), config is loaded into typed, immutable
records instead of scattered mutable fields, and every menu is built on a small reusable click-handler
framework rather than one-off PersistentDataContainer tags. It avoids outdated sync-heavy logic and stays
compatible with next-generation platforms like Folia.

## 📦 Installation

1. Download the latest release
2. Place the `.jar` file into your `/plugins` folder
3. Restart your server
4. Configure the plugin in the generated config files

## ⚙️ Configuration

All features are configurable via `config.yml` and `messages.yml`. Every message supports
[MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting (gradients, hex colors, etc.).

Hotbar items and cosmetics aren't defined in `config.yml` — each one is its own file under
`plugins/LobbySystem/items/hotbar/` or `plugins/LobbySystem/items/cosmetics/` (the filename becomes
the item's id). Add a file to add an item, delete it to remove it. The default install ships example
items covering every available action/kind, including a couple of disabled examples for reference.

## 🕹️ Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/spawn` | — | Teleport to the lobby spawn |
| `/setspawn` | `lobbysystem.set` | Set the lobby spawn at your position |
| `/build` | `lobbysystem.build` | Toggle build mode in the lobby |
| `/lobbysystem reload` | `lobbysystem.reload` | Reload the config without restarting |

Commands you don't have permission for won't show up in tab-completion at all — this is standard
Brigadier permission-gating, not a bug.

## 💬 Support

If you encounter issues or have suggestions, feel free to open an issue or contribute.
