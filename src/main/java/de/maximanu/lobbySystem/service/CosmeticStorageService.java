package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/** Persists each player's equipped hat/particle selection to {@code playerdata/<uuid>.yml}. */
public class CosmeticStorageService {
   private final LobbySystem plugin;
   private final File folder;

   public CosmeticStorageService(LobbySystem plugin) {
      this.plugin = plugin;
      this.folder = new File(plugin.getDataFolder(), "playerdata");
      if (!this.folder.exists()) {
         this.folder.mkdirs();
      }
   }

   public PlayerCosmetics load(UUID uuid) {
      File file = this.file(uuid);
      if (!file.exists()) {
         return new PlayerCosmetics(null, null);
      }

      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      return new PlayerCosmetics(yaml.getString("equipped-hat"), yaml.getString("equipped-particle"));
   }

   public void save(UUID uuid, PlayerCosmetics data) {
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.set("equipped-hat", data.hatId());
      yaml.set("equipped-particle", data.particleId());
      try {
         yaml.save(this.file(uuid));
      } catch (IOException e) {
         this.plugin.getLogger().warning("Failed to save cosmetics for " + uuid + ": " + e.getMessage());
      }
   }

   private File file(UUID uuid) {
      return new File(this.folder, uuid + ".yml");
   }

   public record PlayerCosmetics(String hatId, String particleId) {
   }
}
