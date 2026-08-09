package de.maximanu.lobbySystem.config;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.service.MessageService;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ConfigService {
   private final LobbySystem plugin;
   private final MessageService messageService;
   private volatile PluginConfig config;

   public ConfigService(LobbySystem plugin, MessageService messageService) {
      this.plugin = plugin;
      this.messageService = messageService;
      this.reload();
   }

   public PluginConfig get() {
      return this.config;
   }

   public void playSound(Player player, SoundEffect effect) {
      if (effect != null) {
         effect.play(player, this.config.sounds().enabled());
      }
   }

   public void reload() {
      FileConfiguration source = this.plugin.getConfig();

      String spawnWorldName = source.getString("spawn.location.world", "").trim();
      String lobbyWorldName = source.getString("lobby.world", spawnWorldName).trim();
      if (lobbyWorldName.isEmpty()) {
         lobbyWorldName = spawnWorldName;
      }

      SpawnConfig spawn = new SpawnConfig(
         source.getBoolean("spawn.enabled", true),
         source.getBoolean("spawn.teleport.on-join", true),
         source.getBoolean("spawn.teleport.on-respawn", true),
         source.getBoolean("spawn.teleport.on-void", true)
      );

      ProtectionConfig protection = new ProtectionConfig(
         source.getBoolean("protection.enabled", true),
         source.getBoolean("protection.damage", true),
         source.getBoolean("protection.hunger", true),
         source.getBoolean("protection.block-break", true),
         source.getBoolean("protection.block-place", true),
         source.getBoolean("protection.interact", true),
         source.getBoolean("protection.entity-interact", true),
         source.getBoolean("protection.inventory", true),
         source.getBoolean("protection.item-drop", true),
         source.getBoolean("protection.item-pickup", true),
         source.getBoolean("protection.farmland-trample", true),
         source.getBoolean("protection.mob-spawning", true),
         source.getBoolean("protection.portal-use", true),
         source.getBoolean("protection.buckets", true),
         source.getBoolean("protection.armor-stand-edit", true),
         source.getBoolean("protection.item-frame-rotate", true),
         source.getBoolean("protection.hanging-break", true),
         source.getBoolean("protection.environment.manage-gamerules", true),
         source.getBoolean("protection.environment.weather-lock", true),
         source.getBoolean("protection.environment.time-lock", true),
         Math.floorMod(source.getLong("protection.environment.locked-time", 6000L), 24000L)
      );

      DoubleJumpConfig doubleJump = new DoubleJumpConfig(
         source.getBoolean("double-jump.enabled", true),
         source.getDouble("double-jump.forward-boost", 1.15D),
         source.getDouble("double-jump.upward-boost", 0.82D),
         Math.max(0, source.getInt("double-jump.cooldown-ticks", 30)),
         source.getBoolean("double-jump.xp-bar-cooldown", true)
      );

      BuildModeConfig buildMode = new BuildModeConfig(
         source.getBoolean("build-mode.enabled", true),
         source.getBoolean("build-mode.allow-flight", false),
         source.getBoolean("build-mode.disable-double-jump", true),
         source.getBoolean("build-mode.reset-on-quit", true)
      );

      List<HotbarItemConfig> hotbarItems = this.readHotbarItems(source, "items/hotbar");
      this.warnDuplicateHotbarSlots(hotbarItems);
      HotbarConfig hotbar = new HotbarConfig(source.getBoolean("hotbar.enabled", true), source.getBoolean("hotbar.lock-items", true), hotbarItems);

      int selectorSize = this.normalizeMenuSize(source.getInt("server-selector.menu.size", 27));
      int defaultPrev = Math.max(0, selectorSize - 9);
      int defaultNext = selectorSize - 1;
      int previousPageSlot = this.normalizeMenuSlot(source, "server-selector.menu.previous-page-slot", defaultPrev, selectorSize);
      int nextPageSlot = this.normalizeMenuSlot(source, "server-selector.menu.next-page-slot", defaultNext, selectorSize);
      if (previousPageSlot == nextPageSlot && previousPageSlot != -1) {
         nextPageSlot = defaultNext != previousPageSlot ? defaultNext : -1;
      }

      SelectorConfig selector = new SelectorConfig(
         source.getBoolean("server-selector.enabled", true),
         selectorSize,
         this.normalizeSlots(source.getIntegerList("server-selector.menu.layout-slots"), selectorSize),
         this.materialOrDefault(source.getString("server-selector.menu.filler-material"), Material.LIGHT_GRAY_STAINED_GLASS_PANE),
         previousPageSlot,
         nextPageSlot,
         source.getBoolean("server-selector.menu.fill-empty-slots", true),
         this.readServerEntries(source)
      );

      FeedbackConfig feedback = new FeedbackConfig(
         this.readFeedbackChannel(source, "feedback.spawn-set", FeedbackChannel.CHAT),
         this.readFeedbackChannel(source, "feedback.spawn-teleport", FeedbackChannel.ACTION_BAR),
         this.readFeedbackChannel(source, "feedback.build-mode", FeedbackChannel.ACTION_BAR),
         this.readFeedbackChannel(source, "feedback.visibility-toggle", FeedbackChannel.NONE),
         this.readFeedbackChannel(source, "feedback.server-selector-connect", FeedbackChannel.ACTION_BAR)
      );

      SoundsConfig sounds = new SoundsConfig(
         source.getBoolean("sounds.enabled", true),
         this.readSoundEffect(source, "sounds.entries.double-jump", 1.0F, 1.0F),
         this.readSoundEffect(source, "sounds.entries.double-jump-deny", 0.8F, 0.8F),
         this.readSoundEffect(source, "sounds.entries.selector-open", 1.0F, 1.0F),
         this.readSoundEffect(source, "sounds.entries.info", 1.0F, 1.0F),
         this.readSoundEffect(source, "sounds.entries.hider-toggle", 1.0F, 1.0F),
         this.readSoundEffect(source, "sounds.entries.teleport", 1.0F, 1.0F),
         this.readSoundEffect(source, "sounds.entries.build-mode-enable", 0.9F, 1.2F),
         this.readSoundEffect(source, "sounds.entries.build-mode-disable", 0.9F, 0.9F)
      );

      Map<String, String> linkEntries = new HashMap<>();
      linkEntries.put("website", source.getString("links.entries.website", "https://example.com"));
      linkEntries.put("discord", source.getString("links.entries.discord", "https://discord.gg/example"));
      linkEntries.put("store", source.getString("links.entries.store", "https://store.example.com"));
      LinksConfig links = new LinksConfig(source.getBoolean("links.enabled", true), Map.copyOf(linkEntries));

      JoinConfig join = new JoinConfig(
         source.getBoolean("join.enabled", true),
         source.getBoolean("join.hide-join-message", true),
         source.getBoolean("join.hide-quit-message", true),
         source.getString("join.custom-join-message", "<gradient:#7EE8FA:#5AA9FF>+</gradient> <#E8E8E8>{player}"),
         source.getString("join.custom-quit-message", "<gradient:#FF8FA3:#FFB3C1>-</gradient> <#E8E8E8>{player}")
      );

      CosmeticsConfig cosmetics = new CosmeticsConfig(
         source.getBoolean("cosmetics.enabled", true),
         this.normalizeMenuSize(source.getInt("cosmetics.menu.size", 54)),
         this.readCosmetics("items/cosmetics")
      );

      this.config = new PluginConfig(lobbyWorldName, spawnWorldName, spawn, protection, doubleJump, buildMode, hotbar, selector, feedback, sounds, links, join, cosmetics);
   }

   private List<CosmeticConfig> readCosmetics(String relativeFolder) {
      List<CosmeticConfig> cosmetics = new ArrayList<>();
      for (Map.Entry<String, FileConfiguration> entry : this.loadItemFiles(relativeFolder)) {
         CosmeticConfig cosmetic = this.parseCosmeticItem(entry.getKey(), entry.getValue());
         if (cosmetic != null) {
            cosmetics.add(cosmetic);
         }
      }

      return List.copyOf(cosmetics);
   }

   private CosmeticConfig parseCosmeticItem(String id, ConfigurationSection item) {
      CosmeticKind kind = CosmeticKind.fromConfig(item.getString("kind"), id, this.plugin.getLogger());
      if (kind == null) {
         return null;
      }

      boolean enabled = item.getBoolean("enabled", true);
      Material material = this.materialOrDefault(item.getString("material"), Material.PAPER);
      Component name = this.messageService.deserialize(item.getString("name", id));
      List<Component> lore = item.getStringList("lore").stream().map(this.messageService::deserialize).toList();
      String permission = item.getString("permission", "").trim();
      Particle particle = kind == CosmeticKind.PARTICLE ? this.particleOrNull(item.getString("particle"), id) : null;
      int particleIntervalTicks = Math.max(1, item.getInt("interval-ticks", 5));
      GadgetEffect gadgetEffect = kind == CosmeticKind.GADGET ? GadgetEffect.fromConfig(item.getString("effect"), id, this.plugin.getLogger()) : null;
      int gadgetCooldownTicks = Math.max(0, item.getInt("cooldown-ticks", 100));
      SoundEffect gadgetSound = kind == CosmeticKind.GADGET ? this.readSoundEffect(item, "sound", 1.0F, 1.0F) : null;
      return new CosmeticConfig(id, kind, enabled, material, name, lore, permission, particle, particleIntervalTicks, gadgetEffect, gadgetCooldownTicks, gadgetSound);
   }

   private Particle particleOrNull(String name, String itemId) {
      if (name == null || name.isBlank()) {
         this.plugin.getLogger().warning("Cosmetic '" + itemId + "' has no particle configured. It will have no visible effect.");
         return null;
      }

      try {
         return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         this.plugin.getLogger().warning("Cosmetic '" + itemId + "' has invalid particle '" + name + "'. It will have no visible effect.");
         return null;
      }
   }

   private List<HotbarItemConfig> readHotbarItems(FileConfiguration source, String relativeFolder) {
      List<HotbarItemConfig> items = new ArrayList<>();
      for (Map.Entry<String, FileConfiguration> entry : this.loadItemFiles(relativeFolder)) {
         HotbarItemConfig item = this.parseHotbarItem(entry.getKey(), entry.getValue(), source);
         if (item != null) {
            items.add(item);
         }
      }

      return List.copyOf(items);
   }

   private HotbarItemConfig parseHotbarItem(String id, ConfigurationSection item, FileConfiguration source) {
      HotbarAction action = HotbarAction.fromConfig(item.getString("action"), id, this.plugin.getLogger());
      if (action == null) {
         return null;
      }

      boolean enabled = item.getBoolean("enabled", true);
      if (action == HotbarAction.LINKS) {
         enabled = enabled && source.getBoolean("links.enabled", true);
      } else if (action == HotbarAction.SERVER_SELECTOR) {
         enabled = enabled && source.getBoolean("server-selector.enabled", true);
      }

      int slot = this.normalizeHotbarSlot(item, "slot", id);
      Material material = this.materialOrDefault(item.getString("material"), Material.PAPER);
      Component name = this.messageService.deserialize(item.getString("name", id));
      List<Component> lore = item.getStringList("lore").stream().map(this.messageService::deserialize).toList();
      String value = item.getString("value", "");
      List<ChatLineConfig> message = action == HotbarAction.SHOW_TEXT ? this.readChatLines(item.getMapList("message"), id) : List.of();
      return new HotbarItemConfig(id, enabled, slot, material, name, lore, action, value, message);
   }

   /** Loads every {@code *.yml} file directly inside the plugin data folder's {@code relativeFolder} - the filename (without extension) becomes the item's id. */
   private List<Map.Entry<String, FileConfiguration>> loadItemFiles(String relativeFolder) {
      File folder = new File(this.plugin.getDataFolder(), relativeFolder);
      File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
      if (files == null || files.length == 0) {
         return List.of();
      }

      Arrays.sort(files, Comparator.comparing(File::getName));
      List<Map.Entry<String, FileConfiguration>> result = new ArrayList<>();
      for (File file : files) {
         String id = file.getName().substring(0, file.getName().length() - ".yml".length());
         result.add(Map.entry(id, YamlConfiguration.loadConfiguration(file)));
      }

      return result;
   }

   private List<ChatLineConfig> readChatLines(List<Map<?, ?>> rawLines, String itemId) {
      List<ChatLineConfig> lines = new ArrayList<>();
      for (Map<?, ?> rawLine : rawLines) {
         Object rawText = rawLine.get("text");
         if (rawText == null) {
            continue;
         }

         Component text = this.messageService.deserialize(rawText.toString());
         ChatClickType click = ChatClickType.fromConfig(rawLine.get("click") == null ? null : rawLine.get("click").toString(), itemId, this.plugin.getLogger());
         String value = rawLine.get("value") == null ? "" : rawLine.get("value").toString();
         if (click != ChatClickType.NONE && value.isBlank()) {
            this.plugin.getLogger().warning("Hotbar item '" + itemId + "' has a show-text line with click '" + click.name() + "' but no value. The click will do nothing.");
         }

         Component hover = rawLine.get("hover") == null ? null : this.messageService.deserialize(rawLine.get("hover").toString());
         lines.add(new ChatLineConfig(text, click, value, hover));
      }

      return List.copyOf(lines);
   }

   private List<ServerEntry> readServerEntries(FileConfiguration source) {
      ConfigurationSection section = source.getConfigurationSection("server-selector.servers");
      List<ServerEntry> entries = new ArrayList<>();
      if (section != null) {
         for (String key : section.getKeys(false)) {
            ConfigurationSection server = section.getConfigurationSection(key);
            if (server == null) {
               continue;
            }

            String bungee = server.getString("bungee", "").trim();
            if (bungee.isEmpty()) {
               continue;
            }

            Component display = this.messageService.deserialize(server.getString("name", key));
            Material material = this.materialOrDefault(server.getString("item"), Material.PAPER);
            int slot = server.getInt("slot", -1);
            List<Component> lore = server.contains("lore")
               ? server.getStringList("lore").stream().map(this.messageService::deserialize).toList()
               : this.messageService.componentList("menu.selector.item.default-lore", List.of("&7Click to connect"));
            entries.add(new ServerEntry(display, bungee, material, lore, slot));
         }
      }

      entries.sort(
         Comparator.comparingInt((ServerEntry entry) -> entry.slot() >= 0 ? 0 : 1)
            .thenComparingInt((ServerEntry entry) -> entry.slot() >= 0 ? entry.slot() : Integer.MAX_VALUE)
            .thenComparing(ServerEntry::bungeeName, String.CASE_INSENSITIVE_ORDER)
      );
      return List.copyOf(entries);
   }

   private int normalizeMenuSize(int size) {
      if (size < 9) {
         return 9;
      } else {
         return size > 54 ? 54 : size - size % 9;
      }
   }

   private List<Integer> normalizeSlots(List<Integer> slots, int size) {
      if (slots == null || slots.isEmpty()) {
         return List.of();
      }

      List<Integer> valid = new ArrayList<>();
      for (Integer slot : slots) {
         if (slot != null && slot >= 0 && slot < size && !valid.contains(slot)) {
            valid.add(slot);
         }
      }

      return List.copyOf(valid);
   }

   private Material materialOrDefault(String name, Material fallback) {
      if (name == null || name.isBlank()) {
         return fallback;
      }

      Material material = Material.matchMaterial(name.trim());
      if (material == null) {
         return fallback;
      } else if (!material.isItem() || material == Material.AIR) {
         this.plugin.getLogger().warning("Material '" + name + "' is not a usable item. Using " + fallback + " instead.");
         return fallback;
      } else {
         return material;
      }
   }

   private int normalizeHotbarSlot(ConfigurationSection item, String path, String id) {
      int slot = item.getInt(path, 0);
      if (slot >= 0 && slot <= 8) {
         return slot;
      } else {
         this.plugin.getLogger().warning("Hotbar item '" + id + "' has invalid slot '" + slot + "'. Using 0.");
         return 0;
      }
   }

   private int normalizeMenuSlot(FileConfiguration source, String path, int fallback, int size) {
      int slot = source.getInt(path, fallback);
      if (slot < 0) {
         return -1;
      } else if (slot >= size) {
         this.plugin.getLogger().warning("Invalid menu slot '" + slot + "' at " + path + ". Using " + fallback + ".");
         return fallback;
      } else {
         return slot;
      }
   }

   private void warnDuplicateHotbarSlots(List<HotbarItemConfig> items) {
      List<Integer> enabledSlots = items.stream().filter(HotbarItemConfig::enabled).map(HotbarItemConfig::slot).toList();
      if (enabledSlots.size() != enabledSlots.stream().distinct().count()) {
         this.plugin.getLogger().warning("Hotbar item slots overlap: " + enabledSlots);
      }
   }

   private SoundEffect readSoundEffect(ConfigurationSection source, String basePath, float defaultVolume, float defaultPitch) {
      String name = source.getString(basePath + ".name", "").trim();
      Sound sound = null;
      if (!name.isEmpty() && !name.equalsIgnoreCase("none")) {
         NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
         sound = key == null ? null : Registry.SOUNDS.get(key);
         if (sound == null) {
            sound = Registry.SOUNDS.stream().filter((entry) -> entry.toString().equalsIgnoreCase(name)).findFirst().orElse(null);
         }

         if (sound == null) {
            this.plugin.getLogger().warning("Invalid sound '" + name + "' at " + basePath + ".name. Disabling sound.");
         }
      }

      float volume = source.contains(basePath + ".volume") ? (float) source.getDouble(basePath + ".volume", defaultVolume) : defaultVolume;
      float pitch = source.contains(basePath + ".pitch") ? (float) source.getDouble(basePath + ".pitch", defaultPitch) : defaultPitch;
      return new SoundEffect(sound, volume, pitch);
   }

   private FeedbackChannel readFeedbackChannel(FileConfiguration source, String path, FeedbackChannel fallback) {
      String rawValue = source.getString(path, fallback.getConfigValue());
      return FeedbackChannel.fromConfig(rawValue, fallback, this.plugin.getLogger(), path);
   }
}
