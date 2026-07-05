package de.maximanu.lobbySystem.config;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.service.MessageService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ConfigService {
   private final LobbySystem plugin;
   private final MessageService messageService;
   private String spawnWorldName;
   private String lobbyWorldName;
   private boolean spawnEnabled;
   private boolean teleportOnJoin;
   private boolean teleportOnRespawn;
   private boolean teleportOnVoid;
   private boolean protectionEnabled;
   private boolean protectDamage;
   private boolean protectHunger;
   private boolean protectBlockBreak;
   private boolean protectBlockPlace;
   private boolean protectInteract;
   private boolean protectEntityInteract;
   private boolean protectInventory;
   private boolean protectItemDrop;
   private boolean protectItemPickup;
   private boolean protectFarmlandTrample;
   private boolean protectManageGameRules;
   private boolean protectWeatherChange;
   private boolean protectTimeLock;
   private long lockedTime;
   private boolean protectMobSpawning;
   private boolean protectPortalUse;
   private boolean protectBuckets;
   private boolean protectArmorStandEdit;
   private boolean protectItemFrameRotate;
   private boolean protectHangingBreak;
   private boolean buildModeEnabled;
   private boolean doubleJumpEnabled;
   private double doubleJumpForward;
   private double doubleJumpUp;
   private int doubleJumpCooldownTicks;
   private boolean doubleJumpUseXpBar;
   private boolean hotbarLockEnabled;
   private boolean hotbarEnabled;
   private boolean hotbarInfoEnabled;
   private boolean hotbarSelectorEnabled;
   private boolean hotbarHiderEnabled;
   private boolean selectorMenuEnabled;
   private boolean linksEnabled;
   private boolean buildModeAllowFlight;
   private boolean buildModeDisableDoubleJump;
   private boolean buildModeResetOnQuit;
   private boolean soundsEnabled;
   private FeedbackChannel spawnSetFeedbackChannel;
   private FeedbackChannel spawnTeleportFeedbackChannel;
   private FeedbackChannel buildModeFeedbackChannel;
   private FeedbackChannel visibilityFeedbackChannel;
   private FeedbackChannel selectorConnectFeedbackChannel;
   private Sound soundDoubleJump;
   private float soundDoubleJumpVolume;
   private float soundDoubleJumpPitch;
   private Sound soundDoubleJumpDeny;
   private float soundDoubleJumpDenyVolume;
   private float soundDoubleJumpDenyPitch;
   private Sound soundSelectorOpen;
   private float soundSelectorOpenVolume;
   private float soundSelectorOpenPitch;
   private Sound soundInfo;
   private float soundInfoVolume;
   private float soundInfoPitch;
   private Sound soundHiderToggle;
   private float soundHiderToggleVolume;
   private float soundHiderTogglePitch;
   private Sound soundTeleport;
   private float soundTeleportVolume;
   private float soundTeleportPitch;
   private Sound soundBuildModeEnable;
   private float soundBuildModeEnableVolume;
   private float soundBuildModeEnablePitch;
   private Sound soundBuildModeDisable;
   private float soundBuildModeDisableVolume;
   private float soundBuildModeDisablePitch;
   private Map<String, String> links;
   private List<ServerEntry> serverEntries;
   private Map<String, Integer> hotbarSlots;
   private Map<String, Material> hotbarMaterials;
   private int selectorSize;
   private List<Integer> selectorLayoutSlots;
   private Material selectorFillerMaterial;
   private int selectorPrevSlot;
   private int selectorNextSlot;
   private boolean selectorFillEmpty;

   public ConfigService(LobbySystem plugin, MessageService messageService) {
      this.plugin = plugin;
      this.messageService = messageService;
      this.reload();
   }

   public String getSpawnWorldName() {
      return this.spawnWorldName;
   }

   public String getLobbyWorldName() {
      return this.lobbyWorldName;
   }

   public boolean isSpawnEnabled() {
      return this.spawnEnabled;
   }

   public boolean isTeleportOnJoin() {
      return this.teleportOnJoin;
   }

   public boolean isTeleportOnRespawn() {
      return this.teleportOnRespawn;
   }

   public boolean isTeleportOnVoid() {
      return this.teleportOnVoid;
   }

   public boolean isProtectionEnabled() {
      return this.protectionEnabled;
   }

   public boolean isProtectDamage() {
      return this.protectDamage;
   }

   public boolean isProtectHunger() {
      return this.protectHunger;
   }

   public boolean isProtectBlockBreak() {
      return this.protectBlockBreak;
   }

   public boolean isProtectBlockPlace() {
      return this.protectBlockPlace;
   }

   public boolean isProtectInteract() {
      return this.protectInteract;
   }

   public boolean isProtectEntityInteract() {
      return this.protectEntityInteract;
   }

   public boolean isProtectInventory() {
      return this.protectInventory;
   }

   public boolean isProtectItemDrop() {
      return this.protectItemDrop;
   }

   public boolean isProtectItemPickup() {
      return this.protectItemPickup;
   }

   public boolean isProtectFarmlandTrample() {
      return this.protectFarmlandTrample;
   }

   public boolean isProtectManageGameRules() {
      return this.protectManageGameRules;
   }

   public boolean isProtectWeatherChange() {
      return this.protectWeatherChange;
   }

   public boolean isProtectTimeLock() {
      return this.protectTimeLock;
   }

   public long getLockedTime() {
      return this.lockedTime;
   }

   public boolean isProtectMobSpawning() {
      return this.protectMobSpawning;
   }

   public boolean isProtectPortalUse() {
      return this.protectPortalUse;
   }

   public boolean isProtectBuckets() {
      return this.protectBuckets;
   }

   public boolean isProtectArmorStandEdit() {
      return this.protectArmorStandEdit;
   }

   public boolean isProtectItemFrameRotate() {
      return this.protectItemFrameRotate;
   }

   public boolean isProtectHangingBreak() {
      return this.protectHangingBreak;
   }

   public boolean isBuildModeEnabled() {
      return this.buildModeEnabled;
   }

   public boolean isDoubleJumpEnabled() {
      return this.doubleJumpEnabled;
   }

   public double getDoubleJumpForward() {
      return this.doubleJumpForward;
   }

   public double getDoubleJumpUp() {
      return this.doubleJumpUp;
   }

   public int getDoubleJumpCooldownTicks() {
      return this.doubleJumpCooldownTicks;
   }

   public boolean isDoubleJumpUseXpBar() {
      return this.doubleJumpUseXpBar;
   }

   public boolean isHotbarLockEnabled() {
      return this.hotbarLockEnabled;
   }

   public boolean isHotbarEnabled() {
      return this.hotbarEnabled;
   }

   public boolean isHotbarInfoEnabled() {
      return this.hotbarInfoEnabled;
   }

   public boolean isHotbarSelectorEnabled() {
      return this.hotbarSelectorEnabled;
   }

   public boolean isHotbarHiderEnabled() {
      return this.hotbarHiderEnabled;
   }

   public boolean isSelectorMenuEnabled() {
      return this.selectorMenuEnabled;
   }

   public boolean isLinksEnabled() {
      return this.linksEnabled;
   }

   public boolean isBuildModeAllowFlight() {
      return this.buildModeAllowFlight;
   }

   public boolean isBuildModeDisableDoubleJump() {
      return this.buildModeDisableDoubleJump;
   }

   public boolean isBuildModeResetOnQuit() {
      return this.buildModeResetOnQuit;
   }

   public FeedbackChannel getSpawnSetFeedbackChannel() {
      return this.spawnSetFeedbackChannel;
   }

   public FeedbackChannel getSpawnTeleportFeedbackChannel() {
      return this.spawnTeleportFeedbackChannel;
   }

   public FeedbackChannel getBuildModeFeedbackChannel() {
      return this.buildModeFeedbackChannel;
   }

   public FeedbackChannel getVisibilityFeedbackChannel() {
      return this.visibilityFeedbackChannel;
   }

   public FeedbackChannel getSelectorConnectFeedbackChannel() {
      return this.selectorConnectFeedbackChannel;
   }

   public Sound getSoundDoubleJump() {
      return this.soundDoubleJump;
   }

   public float getSoundDoubleJumpVolume() {
      return this.soundDoubleJumpVolume;
   }

   public float getSoundDoubleJumpPitch() {
      return this.soundDoubleJumpPitch;
   }

   public Sound getSoundDoubleJumpDeny() {
      return this.soundDoubleJumpDeny;
   }

   public float getSoundDoubleJumpDenyVolume() {
      return this.soundDoubleJumpDenyVolume;
   }

   public float getSoundDoubleJumpDenyPitch() {
      return this.soundDoubleJumpDenyPitch;
   }

   public Sound getSoundSelectorOpen() {
      return this.soundSelectorOpen;
   }

   public float getSoundSelectorOpenVolume() {
      return this.soundSelectorOpenVolume;
   }

   public float getSoundSelectorOpenPitch() {
      return this.soundSelectorOpenPitch;
   }

   public Sound getSoundInfo() {
      return this.soundInfo;
   }

   public float getSoundInfoVolume() {
      return this.soundInfoVolume;
   }

   public float getSoundInfoPitch() {
      return this.soundInfoPitch;
   }

   public Sound getSoundHiderToggle() {
      return this.soundHiderToggle;
   }

   public float getSoundHiderToggleVolume() {
      return this.soundHiderToggleVolume;
   }

   public float getSoundHiderTogglePitch() {
      return this.soundHiderTogglePitch;
   }

   public Sound getSoundTeleport() {
      return this.soundTeleport;
   }

   public float getSoundTeleportVolume() {
      return this.soundTeleportVolume;
   }

   public float getSoundTeleportPitch() {
      return this.soundTeleportPitch;
   }

   public Sound getSoundBuildModeEnable() {
      return this.soundBuildModeEnable;
   }

   public float getSoundBuildModeEnableVolume() {
      return this.soundBuildModeEnableVolume;
   }

   public float getSoundBuildModeEnablePitch() {
      return this.soundBuildModeEnablePitch;
   }

   public Sound getSoundBuildModeDisable() {
      return this.soundBuildModeDisable;
   }

   public float getSoundBuildModeDisableVolume() {
      return this.soundBuildModeDisableVolume;
   }

   public float getSoundBuildModeDisablePitch() {
      return this.soundBuildModeDisablePitch;
   }

   public List<ServerEntry> getServerEntries() {
      return this.serverEntries;
   }

   public int getHotbarSlot(String key, int fallback) {
      return (Integer)this.hotbarSlots.getOrDefault(key, fallback);
   }

   public Material getHotbarMaterial(String key, Material fallback) {
      return (Material)this.hotbarMaterials.getOrDefault(key, fallback);
   }

   public int getSelectorSize() {
      return this.selectorSize;
   }

   public List<Integer> getSelectorLayoutSlots() {
      return this.selectorLayoutSlots;
   }

   public Material getSelectorFillerMaterial() {
      return this.selectorFillerMaterial;
   }

   public int getSelectorPrevSlot() {
      return this.selectorPrevSlot;
   }

   public int getSelectorNextSlot() {
      return this.selectorNextSlot;
   }

   public boolean isSelectorFillEmpty() {
      return this.selectorFillEmpty;
   }

   public String getLink(String key, String fallback) {
      return (String)this.links.getOrDefault(key, fallback);
   }

   public void reload() {
      // World scope and feature toggles
      this.spawnWorldName = this.plugin.getConfig().getString("spawn.location.world", "").trim();
      this.lobbyWorldName = this.plugin.getConfig().getString("lobby.world", this.spawnWorldName).trim();
      if (this.lobbyWorldName.isEmpty()) {
         this.lobbyWorldName = this.spawnWorldName;
      }

      this.spawnEnabled = this.plugin.getConfig().getBoolean("spawn.enabled", true);
      this.teleportOnJoin = this.plugin.getConfig().getBoolean("spawn.teleport.on-join", true);
      this.teleportOnRespawn = this.plugin.getConfig().getBoolean("spawn.teleport.on-respawn", true);
      this.teleportOnVoid = this.plugin.getConfig().getBoolean("spawn.teleport.on-void", true);
      this.protectionEnabled = this.plugin.getConfig().getBoolean("protection.enabled", true);
      this.protectDamage = this.plugin.getConfig().getBoolean("protection.damage", true);
      this.protectHunger = this.plugin.getConfig().getBoolean("protection.hunger", true);
      this.protectBlockBreak = this.plugin.getConfig().getBoolean("protection.block-break", true);
      this.protectBlockPlace = this.plugin.getConfig().getBoolean("protection.block-place", true);
      this.protectInteract = this.plugin.getConfig().getBoolean("protection.interact", true);
      this.protectEntityInteract = this.plugin.getConfig().getBoolean("protection.entity-interact", true);
      this.protectInventory = this.plugin.getConfig().getBoolean("protection.inventory", true);
      this.protectItemDrop = this.plugin.getConfig().getBoolean("protection.item-drop", true);
      this.protectItemPickup = this.plugin.getConfig().getBoolean("protection.item-pickup", true);
      this.protectFarmlandTrample = this.plugin.getConfig().getBoolean("protection.farmland-trample", true);
      this.protectMobSpawning = this.plugin.getConfig().getBoolean("protection.mob-spawning", true);
      this.protectPortalUse = this.plugin.getConfig().getBoolean("protection.portal-use", true);
      this.protectBuckets = this.plugin.getConfig().getBoolean("protection.buckets", true);
      this.protectArmorStandEdit = this.plugin.getConfig().getBoolean("protection.armor-stand-edit", true);
      this.protectItemFrameRotate = this.plugin.getConfig().getBoolean("protection.item-frame-rotate", true);
      this.protectHangingBreak = this.plugin.getConfig().getBoolean("protection.hanging-break", true);
      this.protectManageGameRules = this.plugin.getConfig().getBoolean("protection.environment.manage-gamerules", true);
      this.protectWeatherChange = this.plugin.getConfig().getBoolean("protection.environment.weather-lock", true);
      this.protectTimeLock = this.plugin.getConfig().getBoolean("protection.environment.time-lock", true);
      this.lockedTime = Math.floorMod(this.plugin.getConfig().getLong("protection.environment.locked-time", 6000L), 24000L);
      this.buildModeEnabled = this.plugin.getConfig().getBoolean("build-mode.enabled", true);
      this.doubleJumpEnabled = this.plugin.getConfig().getBoolean("double-jump.enabled", true);
      this.doubleJumpForward = this.plugin.getConfig().getDouble("double-jump.forward-boost", 1.15D);
      this.doubleJumpUp = this.plugin.getConfig().getDouble("double-jump.upward-boost", 0.82D);
      this.doubleJumpCooldownTicks = Math.max(0, this.plugin.getConfig().getInt("double-jump.cooldown-ticks", 30));
      this.doubleJumpUseXpBar = this.plugin.getConfig().getBoolean("double-jump.xp-bar-cooldown", true);
      this.hotbarLockEnabled = this.plugin.getConfig().getBoolean("hotbar.lock-items", true);
      this.hotbarEnabled = this.plugin.getConfig().getBoolean("hotbar.enabled", true);
      this.hotbarInfoEnabled = this.plugin.getConfig().getBoolean("hotbar.items.info.enabled", true);
      this.hotbarSelectorEnabled = this.plugin.getConfig().getBoolean("hotbar.items.server-selector.enabled", true);
      this.hotbarHiderEnabled = this.plugin.getConfig().getBoolean("hotbar.items.player-hider.enabled", true);
      this.selectorMenuEnabled = this.plugin.getConfig().getBoolean("server-selector.enabled", true);
      this.linksEnabled = this.plugin.getConfig().getBoolean("links.enabled", true);
      this.buildModeAllowFlight = this.plugin.getConfig().getBoolean("build-mode.allow-flight", false);
      this.buildModeDisableDoubleJump = this.plugin.getConfig().getBoolean("build-mode.disable-double-jump", true);
      this.buildModeResetOnQuit = this.plugin.getConfig().getBoolean("build-mode.reset-on-quit", true);
      this.soundsEnabled = this.plugin.getConfig().getBoolean("sounds.enabled", true);
      this.spawnSetFeedbackChannel = this.readFeedbackChannel("feedback.spawn-set", FeedbackChannel.CHAT);
      this.spawnTeleportFeedbackChannel = this.readFeedbackChannel("feedback.spawn-teleport", FeedbackChannel.ACTION_BAR);
      this.buildModeFeedbackChannel = this.readFeedbackChannel("feedback.build-mode", FeedbackChannel.ACTION_BAR);
      this.visibilityFeedbackChannel = this.readFeedbackChannel("feedback.visibility-toggle", FeedbackChannel.NONE);
      this.selectorConnectFeedbackChannel = this.readFeedbackChannel("feedback.server-selector-connect", FeedbackChannel.ACTION_BAR);

      // Sound palette
      this.soundDoubleJump = this.readSound("sounds.entries.double-jump.name");
      this.soundDoubleJumpVolume = this.readFloat("sounds.entries.double-jump.volume", 1.0F);
      this.soundDoubleJumpPitch = this.readFloat("sounds.entries.double-jump.pitch", 1.0F);
      this.soundDoubleJumpDeny = this.readSound("sounds.entries.double-jump-deny.name");
      this.soundDoubleJumpDenyVolume = this.readFloat("sounds.entries.double-jump-deny.volume", 0.8F);
      this.soundDoubleJumpDenyPitch = this.readFloat("sounds.entries.double-jump-deny.pitch", 0.8F);
      this.soundSelectorOpen = this.readSound("sounds.entries.selector-open.name");
      this.soundSelectorOpenVolume = this.readFloat("sounds.entries.selector-open.volume", 1.0F);
      this.soundSelectorOpenPitch = this.readFloat("sounds.entries.selector-open.pitch", 1.0F);
      this.soundInfo = this.readSound("sounds.entries.info.name");
      this.soundInfoVolume = this.readFloat("sounds.entries.info.volume", 1.0F);
      this.soundInfoPitch = this.readFloat("sounds.entries.info.pitch", 1.0F);
      this.soundHiderToggle = this.readSound("sounds.entries.hider-toggle.name");
      this.soundHiderToggleVolume = this.readFloat("sounds.entries.hider-toggle.volume", 1.0F);
      this.soundHiderTogglePitch = this.readFloat("sounds.entries.hider-toggle.pitch", 1.0F);
      this.soundTeleport = this.readSound("sounds.entries.teleport.name");
      this.soundTeleportVolume = this.readFloat("sounds.entries.teleport.volume", 1.0F);
      this.soundTeleportPitch = this.readFloat("sounds.entries.teleport.pitch", 1.0F);
      this.soundBuildModeEnable = this.readSound("sounds.entries.build-mode-enable.name");
      this.soundBuildModeEnableVolume = this.readFloat("sounds.entries.build-mode-enable.volume", 0.9F);
      this.soundBuildModeEnablePitch = this.readFloat("sounds.entries.build-mode-enable.pitch", 1.2F);
      this.soundBuildModeDisable = this.readSound("sounds.entries.build-mode-disable.name");
      this.soundBuildModeDisableVolume = this.readFloat("sounds.entries.build-mode-disable.volume", 0.9F);
      this.soundBuildModeDisablePitch = this.readFloat("sounds.entries.build-mode-disable.pitch", 0.9F);

      // External links and selector entries
      Map<String, String> linkMap = new HashMap<>();
      linkMap.put("website", this.plugin.getConfig().getString("links.entries.website", "https://example.com"));
      linkMap.put("discord", this.plugin.getConfig().getString("links.entries.discord", "https://discord.gg/example"));
      linkMap.put("store", this.plugin.getConfig().getString("links.entries.store", "https://store.example.com"));
      this.links = Collections.unmodifiableMap(linkMap);
      ConfigurationSection sec = this.plugin.getConfig().getConfigurationSection("server-selector.servers");
      List<ServerEntry> entries = new ArrayList<>();
      if (sec != null) {
         Iterator var4 = sec.getKeys(false).iterator();

         while(var4.hasNext()) {
            String key = (String)var4.next();
            ConfigurationSection server = sec.getConfigurationSection(key);
            if (server != null) {
               String bungee = server.getString("bungee", "").trim();
               if (!bungee.isEmpty()) {
                  Component display = this.messageService.deserialize(server.getString("name", key));
                  String itemName = server.getString("item", "PAPER");
                  Material mat = Material.matchMaterial(itemName);
                  if (mat == null) {
                     mat = Material.PAPER;
                  }

                  int slot = server.getInt("slot", -1);
                  List<Component> lore;
                  if (!server.contains("lore")) {
                     lore = this.messageService.componentList("menu.selector.item.default-lore", List.of("&7Click to connect"));
                  } else {
                     lore = server.getStringList("lore").stream().map(this.messageService::deserialize).toList();
                  }

                  List<Component> finalLore = new ArrayList<>(lore);
                  entries.add(new ServerEntry(display, bungee, mat, finalLore, slot));
               }
            }
         }
      }

      entries.sort(Comparator.comparingInt((ServerEntry entry) -> {
         return entry.getSlot() >= 0 ? 0 : 1;
      }).thenComparingInt((ServerEntry entry) -> {
         return entry.getSlot() >= 0 ? entry.getSlot() : Integer.MAX_VALUE;
      }).thenComparing(ServerEntry::getBungeeName, String.CASE_INSENSITIVE_ORDER));
      this.serverEntries = Collections.unmodifiableList(entries);

      // Hotbar and menu layout
      this.hotbarSlots = new HashMap<>();
      this.hotbarSlots.put("info", this.normalizeHotbarSlot("hotbar.items.info.slot", 0));
      this.hotbarSlots.put("selector", this.normalizeHotbarSlot("hotbar.items.server-selector.slot", 4));
      this.hotbarSlots.put("hider", this.normalizeHotbarSlot("hotbar.items.player-hider.slot", 8));
      this.warnDuplicateHotbarSlots();
      this.hotbarMaterials = new HashMap<>();
      this.hotbarMaterials.put("info", this.materialOrDefault(this.plugin.getConfig().getString("hotbar.items.info.material"), Material.BOOK));
      this.hotbarMaterials.put("selector", this.materialOrDefault(this.plugin.getConfig().getString("hotbar.items.server-selector.material"), Material.COMPASS));
      this.hotbarMaterials.put("hider", this.materialOrDefault(this.plugin.getConfig().getString("hotbar.items.player-hider.material"), Material.PLAYER_HEAD));
      this.selectorSize = this.normalizeMenuSize(this.plugin.getConfig().getInt("server-selector.menu.size", 27));
      this.selectorLayoutSlots = this.normalizeSlots(this.plugin.getConfig().getIntegerList("server-selector.menu.layout-slots"), this.selectorSize);
      this.selectorFillerMaterial = this.materialOrDefault(this.plugin.getConfig().getString("server-selector.menu.filler-material"), Material.LIGHT_GRAY_STAINED_GLASS_PANE);
      this.selectorFillEmpty = this.plugin.getConfig().getBoolean("server-selector.menu.fill-empty-slots", true);
      int defaultPrev = Math.max(0, this.selectorSize - 9);
      int defaultNext = this.selectorSize - 1;
      this.selectorPrevSlot = this.normalizeMenuSlot("server-selector.menu.previous-page-slot", defaultPrev, this.selectorSize);
      this.selectorNextSlot = this.normalizeMenuSlot("server-selector.menu.next-page-slot", defaultNext, this.selectorSize);
      if (this.selectorPrevSlot == this.selectorNextSlot && this.selectorPrevSlot != -1) {
         this.selectorNextSlot = defaultNext != this.selectorPrevSlot ? defaultNext : -1;
      }
   }

   private int normalizeMenuSize(int size) {
      if (size < 9) {
         return 9;
      } else {
         return size > 54 ? 54 : size - size % 9;
      }
   }

   private List<Integer> normalizeSlots(List<Integer> slots, int size) {
      if (slots != null && !slots.isEmpty()) {
         List<Integer> valid = new ArrayList<>();
         Iterator var4 = slots.iterator();

         while(var4.hasNext()) {
            Integer slot = (Integer)var4.next();
            if (slot != null && slot >= 0 && slot < size && !valid.contains(slot)) {
               valid.add(slot);
            }
         }

         return Collections.unmodifiableList(valid);
      } else {
         return List.of();
      }
   }

   private Material materialOrDefault(String name, Material fallback) {
      if (name != null && !name.isBlank()) {
         Material mat = Material.matchMaterial(name.trim());
         if (mat == null) {
            return fallback;
         } else if (!mat.isItem() || mat == Material.AIR) {
            this.plugin.getLogger().warning("Material '" + name + "' is not a usable item. Using " + fallback + " instead.");
            return fallback;
         } else {
            return mat;
         }
      } else {
         return fallback;
      }
   }

   private int normalizeHotbarSlot(String path, int fallback) {
      int slot = this.plugin.getConfig().getInt(path, fallback);
      if (slot >= 0 && slot <= 8) {
         return slot;
      } else {
         this.plugin.getLogger().warning("Invalid hotbar slot '" + slot + "' at " + path + ". Using " + fallback + ".");
         return fallback;
      }
   }

   private int normalizeMenuSlot(String path, int fallback, int size) {
      int slot = this.plugin.getConfig().getInt(path, fallback);
      if (slot < 0) {
         return -1;
      } else if (slot >= size) {
         this.plugin.getLogger().warning("Invalid menu slot '" + slot + "' at " + path + ". Using " + fallback + ".");
         return fallback;
      } else {
         return slot;
      }
   }

   private void warnDuplicateHotbarSlots() {
      List<Integer> enabledSlots = new ArrayList<>();
      if (this.hotbarInfoEnabled) {
         enabledSlots.add(this.hotbarSlots.getOrDefault("info", 0));
      }

      if (this.hotbarSelectorEnabled) {
         enabledSlots.add(this.hotbarSlots.getOrDefault("selector", 4));
      }

      if (this.hotbarHiderEnabled) {
         enabledSlots.add(this.hotbarSlots.getOrDefault("hider", 8));
      }

      if (enabledSlots.size() != enabledSlots.stream().distinct().count()) {
         int info = this.hotbarSlots.getOrDefault("info", 0);
         int selector = this.hotbarSlots.getOrDefault("selector", 4);
         int hider = this.hotbarSlots.getOrDefault("hider", 8);
         this.plugin.getLogger().warning("Hotbar slots overlap (info=" + info + ", selector=" + selector + ", hider=" + hider + ").");
      }
   }

   private Sound readSound(String path) {
      String name = this.plugin.getConfig().getString(path, "").trim();
      if (!name.isEmpty() && !name.equalsIgnoreCase("none")) {
         NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
         Sound sound = key == null ? null : (Sound)Registry.SOUNDS.get(key);
         if (sound == null) {
            sound = Registry.SOUNDS.stream().filter((entry) -> {
               return entry.toString().equalsIgnoreCase(name);
            }).findFirst().orElse(null);
         }

         if (sound == null) {
            this.plugin.getLogger().warning("Invalid sound '" + name + "' at " + path + ". Disabling sound.");
         }

         return sound;
      } else {
         return null;
      }
   }

   private float readFloat(String path, float fallback) {
      return !this.plugin.getConfig().contains(path) ? fallback : (float)this.plugin.getConfig().getDouble(path, (double)fallback);
   }

   private FeedbackChannel readFeedbackChannel(String path, FeedbackChannel fallback) {
      String rawValue = this.plugin.getConfig().getString(path, fallback.getConfigValue());
      return FeedbackChannel.fromConfig(rawValue, fallback, this.plugin.getLogger(), path);
   }

   public void playSound(Player player, Sound sound, float volume, float pitch) {
      if (this.soundsEnabled && player != null && sound != null) {
         player.playSound(player.getLocation(), sound, volume, pitch);
      }
   }

}
