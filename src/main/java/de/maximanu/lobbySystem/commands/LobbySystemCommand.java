package de.maximanu.lobbySystem.commands;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class LobbySystemCommand implements CommandExecutor {
   private final LobbySystem plugin;

   public LobbySystemCommand(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         sender.sendMessage(this.plugin.getMessageService().component("usage.lobbysystem", "Usage: /lobbysystem reload"));
         return true;
      } else if (args[0].equalsIgnoreCase("reload")) {
         if (sender instanceof Player) {
            Player p = (Player)sender;
            if (!p.isOp() && !p.hasPermission("lobbysystem.reload")) {
               p.sendMessage(this.plugin.getMessageService().component("errors.no-permission.reload", "&cYou don't have permission to reload the config."));
               return true;
            }
         } else if (sender instanceof ConsoleCommandSender) {
         }

         this.plugin.reloadPluginConfig();
         this.plugin.getPlayerListener().refreshAllPlayers();
         sender.sendMessage(this.plugin.getMessageService().component("info.config-reloaded", "&aConfig reloaded."));
         return true;
      } else {
         sender.sendMessage(this.plugin.getMessageService().component("usage.lobbysystem", "Usage: /lobbysystem reload"));
         return true;
      }
   }
}
