package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.Location;
import org.bukkit.World;

public class SpawnService {
   private final LobbySystem plugin;
   private Location cachedSpawnLocation;

   public SpawnService(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public void saveSpawnLocation(Location loc) {
      if (loc != null && loc.getWorld() != null) {
         this.plugin.getConfig().set("spawn.world", loc.getWorld().getName());
         this.plugin.getConfig().set("spawn.x", loc.getX());
         this.plugin.getConfig().set("spawn.y", loc.getY());
         this.plugin.getConfig().set("spawn.z", loc.getZ());
         this.plugin.getConfig().set("spawn.yaw", loc.getYaw());
         this.plugin.getConfig().set("spawn.pitch", loc.getPitch());
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
         this.plugin.getLogger().warning("Spawn world '" + worldName + "' is not loaded. Spawn teleportation is disabled.");
         this.cachedSpawnLocation = null;
         return;
      }

      double x = this.plugin.getConfig().getDouble("spawn.x");
      double y = this.plugin.getConfig().getDouble("spawn.y");
      double z = this.plugin.getConfig().getDouble("spawn.z");
      float yaw = (float)this.plugin.getConfig().getDouble("spawn.yaw");
      float pitch = (float)this.plugin.getConfig().getDouble("spawn.pitch");
      this.cachedSpawnLocation = new Location(world, x, y, z, yaw, pitch);
   }
}
