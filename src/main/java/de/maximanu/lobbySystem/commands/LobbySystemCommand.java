package de.maximanu.lobbySystem.commands;

import de.maximanu.lobbySystem.LobbySystem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class LobbySystemCommand implements CommandExecutor, TabCompleter {
   private final LobbySystem plugin;

   public LobbySystemCommand(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         sender.sendMessage(this.plugin.getMessageService().component("usage.lobbysystem", "Usage: /lobbysystem reload"));
         return true;
      } else if (args[0].equalsIgnoreCase("reload")) {
         if (!this.canReload(sender)) {
            sender.sendMessage(this.plugin.getMessageService().component("errors.no-permission.reload", "&cYou don't have permission to reload the config."));
            return true;
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

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1 && this.canReload(sender)) {
         return StringUtil.copyPartialMatches(args[0], List.of("reload"), new ArrayList<>());
      }

      return List.of();
   }

   private boolean canReload(CommandSender sender) {
      return !(sender instanceof Player player) || player.isOp() || player.hasPermission("lobbysystem.reload");
   }
}
