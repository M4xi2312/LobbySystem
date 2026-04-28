package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LobbyWorldService {
   private final LobbySystem plugin;
   private final MessageService messageService;

   public LobbyWorldService(LobbySystem plugin) {
      this.plugin = plugin;
      this.messageService = plugin.getMessageService();
   }

   public boolean isLobbyWorld(Player player) {
      return player != null && this.isLobbyWorld(player.getWorld());
   }

   public boolean isLobbyWorld(World world) {
      if (world == null) {
         return false;
      }

      String lobbyWorldName = this.plugin.getConfigService().getLobbyWorldName();
      return lobbyWorldName.isEmpty() || world.getName().equalsIgnoreCase(lobbyWorldName);
   }

   public void sendLobbyWorldOnlyMessage(Player player) {
      if (player != null) {
         player.sendMessage(this.messageService.component("errors.lobby-world-only", "<#FF5C5C>Error <#D6D6D6>This feature only works in the configured lobby world."));
      }
   }
}
