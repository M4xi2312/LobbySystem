package de.maximanu.lobbySystem.commands;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildCommand implements CommandExecutor {
   private final LobbySystem plugin;

   public BuildCommand(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.plugin.getMessageService().get("errors.only-players", "&cOnly players can use this command."));
         return true;
      }

      Player player = (Player)sender;
      if (!this.plugin.getConfigService().isBuildModeEnabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "build mode");
         return true;
      }

      if (!this.plugin.getLobbyWorldService().isLobbyWorld(player)) {
         this.plugin.getLobbyWorldService().sendLobbyWorldOnlyMessage(player);
         return true;
      }

      if (!player.isOp() && !player.hasPermission("lobbysystem.build")) {
         player.sendMessage(this.plugin.getMessageService().get("errors.no-permission.build", "&cYou don't have permission to toggle build mode."));
         return true;
      }

      this.plugin.getBuildModeService().toggle(player);
      this.plugin.getPlayerListener().refreshPlayer(player);
      return true;
   }
}
