# MLobbySystem

A modern, lightweight lobby system for Paper servers — built with performance and flexibility in mind.
Designed for current server setups, including full support for Folia's regionized multithreading model.

## ✨ Features

* ⚡ **Folia Support** – Built to run safely and efficiently on Folia servers
* 🧭 **Custom Spawn System** – Define and manage your lobby spawn with ease
* 🎮 **Join Management** – Custom or silenced join/quit messages, teleport-on-join/respawn/void
* 🧰 **Item-Based Actions** – Configure lobby items with custom actions (info, server selector, player hider)
* 🖥️ **Server Selector Menu** – Paginated, MiniMessage-styled proxy server selector
* 🚫 **Lobby Protection** – Disable damage, hunger, block breaking, and more
* 🔄 **Auto Teleport** – Send players to spawn automatically on join, respawn, or void fall
* 🎛️ **Highly Configurable** – Simple config structure for fast setup and customization

## 🧩 Compatibility

* ✅ Paper (1.21.11+)
* ✅ Folia (fully supported)

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
