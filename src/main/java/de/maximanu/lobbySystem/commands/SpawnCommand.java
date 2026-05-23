package de.maximanu.lobbySystem.commands;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
   private final LobbySystem plugin;

   public SpawnCommand(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.plugin.getMessageService().get("errors.only-players", "&cOnly players can use this command."));
         return true;
      }

      Player player = (Player)sender;
      if (!this.plugin.getConfigService().isSpawnEnabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "spawn");
         return true;
      }

      this.plugin.getLobbyPlayerService().teleportToSpawnIfSet(player);
      return true;
   }
}
