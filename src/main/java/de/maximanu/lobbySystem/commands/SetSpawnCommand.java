package de.maximanu.lobbySystem.commands;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {
   private final LobbySystem plugin;

   public SetSpawnCommand(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.plugin.getMessageService().get("errors.only-players", "&cOnly players can use this command."));
         return true;
      }

      Player player = (Player)sender;
      if (!this.plugin.getConfigService().isSpawnEnabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "setspawn");
         return true;
      }

      if (!this.plugin.getLobbyWorldService().isLobbyWorld(player)) {
         this.plugin.getLobbyWorldService().sendLobbyWorldOnlyMessage(player);
         return true;
      }

      if (!player.isOp() && !player.hasPermission("lobbysystem.set")) {
         player.sendMessage(this.plugin.getMessageService().get("errors.no-permission.setspawn", "&cYou don't have permission to set spawn."));
         return true;
      }

      this.plugin.getSpawnService().saveSpawnLocation(player.getLocation());
      this.plugin.getConfigService().getSpawnSetFeedbackChannel().send(
         player,
         this.plugin.getMessageService().component("info.spawn-set", "<gradient:#7DFF9C:#B8FFCC>Spawn saved</gradient> <#D6D6D6>The lobby location has been updated."),
         this.plugin.getMessageService().component("actionbar.spawn.set", "<gradient:#7DFF9C:#B8FFCC>Spawn saved")
      );
      return true;
   }
}
