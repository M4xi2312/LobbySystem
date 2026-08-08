package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.CosmeticConfig;
import de.maximanu.lobbySystem.config.CosmeticKind;
import de.maximanu.lobbySystem.service.CosmeticStorageService.PlayerCosmetics;
import de.maximanu.lobbySystem.util.ItemFactory;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Applies/toggles hats, particle trails, and gadgets, and persists the player's selection. */
public class CosmeticService {
   // Capped so a long-range hook pull can't exceed a normal elytra-firework-boost speed - much
   // more than this risks the client's own anti-cheat-adjacent movement checks rejecting the jump.
   private static final double MAX_HOOK_SPEED = 2.4;

   private final LobbySystem plugin;
   private final CosmeticStorageService storageService;
   private final NamespacedKey hatKey;
   private final NamespacedKey gadgetKey;
   private final Map<UUID, PlayerCosmetics> equipped = new ConcurrentHashMap<>();
   private final Map<UUID, ScheduledTask> particleTasks = new ConcurrentHashMap<>();
   private final Map<UUID, Long> gadgetCooldowns = new ConcurrentHashMap<>();

   public CosmeticService(LobbySystem plugin) {
      this.plugin = plugin;
      this.storageService = new CosmeticStorageService(plugin);
      this.hatKey = new NamespacedKey(plugin, "cosmetic_hat");
      this.gadgetKey = new NamespacedKey(plugin, "cosmetic_gadget");
   }

   // Toggling from the menu. Both validate the cosmetic (and, for hats, that it could actually be
   // put on) before persisting, so storage never claims something is equipped that isn't really worn.
   public void toggleHat(Player player, String id) {
      if (!this.isUsable(id, CosmeticKind.HAT, player)) {
         return;
      }

      PlayerCosmetics current = this.getOrLoad(player.getUniqueId());
      String newHat = id.equals(current.hatId()) ? null : id;
      if (!this.applyHat(player, newHat)) {
         player.sendMessage(this.plugin.getMessageService().component("errors.cosmetics.helmet-occupied", "<#FF5C5C>Error <#D6D6D6>Remove your current helmet first."));
         return;
      }

      this.store(player.getUniqueId(), new PlayerCosmetics(newHat, current.particleId()));
   }

   public void toggleParticle(Player player, String id) {
      if (!this.isUsable(id, CosmeticKind.PARTICLE, player)) {
         return;
      }

      PlayerCosmetics current = this.getOrLoad(player.getUniqueId());
      String newParticle = id.equals(current.particleId()) ? null : id;
      this.store(player.getUniqueId(), new PlayerCosmetics(current.hatId(), newParticle));
      this.applyParticle(player, newParticle);
   }

   public String getEquippedHat(Player player) {
      return this.getOrLoad(player.getUniqueId()).hatId();
   }

   public String getEquippedParticle(Player player) {
      return this.getOrLoad(player.getUniqueId()).particleId();
   }

   // Gadgets
   public void giveGadget(Player player, String id) {
      if (!this.isUsable(id, CosmeticKind.GADGET, player)) {
         return;
      }

      CosmeticConfig cosmetic = this.find(id, CosmeticKind.GADGET).orElseThrow();
      ItemStack item = ItemFactory.createNamedItem(cosmetic.material(), cosmetic.name(), cosmetic.lore());
      item.editMeta((meta) -> meta.getPersistentDataContainer().set(this.gadgetKey, PersistentDataType.STRING, id));
      Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
      if (!overflow.isEmpty()) {
         player.sendMessage(this.plugin.getMessageService().component("errors.cosmetics.inventory-full", "<#FF5C5C>Error <#D6D6D6>Your inventory is full."));
      }
   }

   public boolean isGadget(ItemStack item) {
      return this.getGadgetId(item) != null;
   }

   public String getGadgetId(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return null;
      }

      ItemMeta meta = item.getItemMeta();
      return meta == null ? null : meta.getPersistentDataContainer().get(this.gadgetKey, PersistentDataType.STRING);
   }

   public void useGadget(Player player, String id) {
      if (!this.isUsable(id, CosmeticKind.GADGET, player)) {
         return;
      }

      CosmeticConfig cosmetic = this.find(id, CosmeticKind.GADGET).orElseThrow();
      long now = System.currentTimeMillis();
      long nextAllowed = this.gadgetCooldowns.getOrDefault(player.getUniqueId(), 0L);
      if (nextAllowed > now) {
         player.sendActionBar(this.plugin.getMessageService().component("errors.cosmetics.gadget-cooldown", "<#FF5C5C>Wait a moment before using this again."));
         return;
      }

      this.gadgetCooldowns.put(player.getUniqueId(), now + cosmetic.gadgetCooldownTicks() * 50L);
      this.playGadgetEffect(player, cosmetic);
   }

   // Per-player lobby state sync - called from LobbyPlayerService.refreshPlayer
   public void reapply(Player player) {
      boolean active = this.plugin.getConfigService().get().cosmetics().enabled() && this.plugin.getLobbyWorldService().isLobbyWorld(player);
      if (!active) {
         this.applyHat(player, null);
         this.stopParticleTask(player.getUniqueId());
         return;
      }

      PlayerCosmetics data = this.getOrLoad(player.getUniqueId());
      this.applyHat(player, data.hatId());
      this.applyParticle(player, data.particleId());
   }

   public void handleQuit(Player player) {
      this.stopParticleTask(player.getUniqueId());
      this.equipped.remove(player.getUniqueId());
      this.gadgetCooldowns.remove(player.getUniqueId());
   }

   /**
    * Applies the given hat (or clears it, if {@code hatId} is null or no longer valid - e.g. its
    * permission was revoked or it was disabled/removed from config since it was last equipped).
    * Never touches a helmet we didn't put there ourselves. Returns false only when a hat was
    * requested but couldn't be equipped because a real (non-cosmetic) helmet is already worn -
    * callers use that to decide whether to persist the new selection.
    */
   private boolean applyHat(Player player, String hatId) {
      ItemStack currentHelmet = player.getInventory().getHelmet();
      boolean currentIsOurs = this.isTaggedHat(currentHelmet);

      Optional<CosmeticConfig> cosmetic = hatId == null ? Optional.empty() : this.find(hatId, CosmeticKind.HAT);
      boolean valid = cosmetic.isPresent() && cosmetic.get().enabled() && cosmetic.get().isUnlockedFor(player);

      if (!valid) {
         if (currentIsOurs) {
            player.getInventory().setHelmet(null);
         }

         return true;
      }

      if (currentHelmet != null && !currentIsOurs) {
         return false;
      }

      ItemStack hatItem = ItemFactory.createNamedItem(cosmetic.get().material(), cosmetic.get().name(), cosmetic.get().lore());
      hatItem.editMeta((meta) -> meta.getPersistentDataContainer().set(this.hatKey, PersistentDataType.STRING, hatId));
      player.getInventory().setHelmet(hatItem);
      return true;
   }

   private boolean isTaggedHat(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return false;
      }

      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.getPersistentDataContainer().has(this.hatKey, PersistentDataType.STRING);
   }

   // Particle trail
   private void applyParticle(Player player, String particleId) {
      this.stopParticleTask(player.getUniqueId());
      if (particleId == null) {
         return;
      }

      Optional<CosmeticConfig> cosmeticOptional = this.find(particleId, CosmeticKind.PARTICLE);
      if (cosmeticOptional.isEmpty() || !cosmeticOptional.get().enabled() || !cosmeticOptional.get().isUnlockedFor(player)) {
         return;
      }

      CosmeticConfig cosmetic = cosmeticOptional.get();
      Particle particle = cosmetic.particle();
      if (particle == null) {
         return;
      }

      ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, (scheduledTask) -> {
         if (!player.isOnline() || !this.plugin.getLobbyWorldService().isLobbyWorld(player)) {
            scheduledTask.cancel();
            return;
         }

         player.getWorld().spawnParticle(particle, player.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
      }, () -> this.particleTasks.remove(player.getUniqueId()), 1L, cosmetic.particleIntervalTicks());
      this.particleTasks.put(player.getUniqueId(), task);
   }

   private void stopParticleTask(UUID uuid) {
      ScheduledTask task = this.particleTasks.remove(uuid);
      if (task != null) {
         task.cancel();
      }
   }

   // Gadget effect
   private void playGadgetEffect(Player player, CosmeticConfig cosmetic) {
      switch (cosmetic.gadgetEffect()) {
         case FIREWORK -> this.playFireworkEffect(player);
         case GRAPPLING_HOOK -> this.playGrapplingHookEffect(player);
         case PEARL_BOW -> this.playPearlBowEffect(player);
      }

      if (cosmetic.gadgetSound() != null) {
         this.plugin.getConfigService().playSound(player, cosmetic.gadgetSound());
      }
   }

   private void playFireworkEffect(Player player) {
      Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
      FireworkMeta meta = firework.getFireworkMeta();
      meta.addEffect(FireworkEffect.builder().withColor(Color.YELLOW).with(FireworkEffect.Type.BURST).trail(false).flicker(false).build());
      meta.setPower(0);
      firework.setFireworkMeta(meta);
      firework.detonate();
   }

   // Pulls the player toward whatever block they're looking at (or a fixed max distance if none is
   // hit) - a simple velocity-based "hookshot" rather than a tracked fishing-rod projectile.
   private void playGrapplingHookEffect(Player player) {
      double maxDistance = 40.0;
      RayTraceResult hit = player.rayTraceBlocks(maxDistance);
      Location target = hit != null
         ? hit.getHitPosition().toLocation(player.getWorld())
         : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(maxDistance));

      Vector toTarget = target.toVector().subtract(player.getLocation().toVector());
      double distance = toTarget.length();
      if (distance < 1.0) {
         return;
      }

      double speed = Math.min(MAX_HOOK_SPEED, 0.3 + distance * 0.08);
      player.setVelocity(toTarget.normalize().multiply(speed));
   }

   // Launches an ender pearl - vanilla teleport-on-land behavior does the rest. Fall damage on
   // landing is already suppressed by the lobby's protection.damage setting.
   private void playPearlBowEffect(Player player) {
      player.launchProjectile(EnderPearl.class);
   }

   // Lookup helpers
   public Optional<CosmeticConfig> find(String id, CosmeticKind kind) {
      return this.plugin.getConfigService().get().cosmetics().items().stream().filter((item) -> item.id().equals(id) && item.kind() == kind).findFirst();
   }

   public Optional<CosmeticConfig> findById(String id) {
      return this.plugin.getConfigService().get().cosmetics().items().stream().filter((item) -> item.id().equals(id)).findFirst();
   }

   /** True if the cosmetic exists, is enabled, and the player is permitted to use it right now. */
   private boolean isUsable(String id, CosmeticKind kind, Player player) {
      return this.find(id, kind).filter((cosmetic) -> cosmetic.enabled() && cosmetic.isUnlockedFor(player)).isPresent();
   }

   private PlayerCosmetics getOrLoad(UUID uuid) {
      return this.equipped.computeIfAbsent(uuid, this.storageService::load);
   }

   private void store(UUID uuid, PlayerCosmetics data) {
      this.equipped.put(uuid, data);
      this.storageService.save(uuid, data);
   }
}
