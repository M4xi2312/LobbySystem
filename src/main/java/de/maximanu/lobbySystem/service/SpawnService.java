package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.Location;
import org.bukkit.World;

public class SpawnService {
   private final LobbySystem plugin;
   private Location cachedSpawnLocation;
   private String lastMissingWorldName;

   public SpawnService(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public void saveSpawnLocation(Location loc) {
      if (loc != null && loc.getWorld() != null) {
         this.plugin.getConfig().set("spawn.location.world", loc.getWorld().getName());
         this.plugin.getConfig().set("spawn.location.x", loc.getX());
         this.plugin.getConfig().set("spawn.location.y", loc.getY());
         this.plugin.getConfig().set("spawn.location.z", loc.getZ());
         this.plugin.getConfig().set("spawn.location.yaw", loc.getYaw());
         this.plugin.getConfig().set("spawn.location.pitch", loc.getPitch());
         if (this.plugin.getConfig().getString("lobby.world", "").isBlank()) {
            this.plugin.getConfig().set("lobby.world", loc.getWorld().getName());
         }

         this.plugin.saveConfig();
         this.plugin.getConfigService().reload();
         this.reload();
      }
   }

   public Location getSpawnLocation() {
      if (this.cachedSpawnLocation == null && !this.plugin.getConfigService().getSpawnWorldName().isBlank()) {
         this.reload();
      }

      return this.cachedSpawnLocation == null ? null : this.cachedSpawnLocation.clone();
   }

   public void reload() {
      String worldName = this.plugin.getConfigService().getSpawnWorldName();
      if (worldName.isEmpty()) {
         this.cachedSpawnLocation = null;
         return;
      }

      World world = this.plugin.getServer().getWorld(worldName);
      if (world == null) {
         if (!worldName.equals(this.lastMissingWorldName)) {
            this.plugin.getLogger().warning("Spawn world '" + worldName + "' is not loaded. Spawn teleportation is disabled.");
            this.lastMissingWorldName = worldName;
         }

         this.cachedSpawnLocation = null;
         return;
      }

      this.lastMissingWorldName = null;
      double x = this.plugin.getConfig().getDouble("spawn.location.x");
      double y = this.plugin.getConfig().getDouble("spawn.location.y");
      double z = this.plugin.getConfig().getDouble("spawn.location.z");
      float yaw = (float)this.plugin.getConfig().getDouble("spawn.location.yaw");
      float pitch = (float)this.plugin.getConfig().getDouble("spawn.location.pitch");
      this.cachedSpawnLocation = new Location(world, x, y, z, yaw, pitch);
   }
}
