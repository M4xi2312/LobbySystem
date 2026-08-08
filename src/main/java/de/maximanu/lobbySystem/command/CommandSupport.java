package de.maximanu.lobbySystem.command;

import de.maximanu.lobbySystem.LobbySystem;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class CommandSupport {
   private CommandSupport() {
   }

   /** Returns the invoking player, or sends the "players only" message and returns null. */
   static Player requirePlayer(LobbySystem plugin, CommandSourceStack source) {
      CommandSender sender = source.getSender();
      if (sender instanceof Player player) {
         return player;
      }

      sender.sendMessage(plugin.getMessageService().component("errors.only-players", "<#FF5C5C>Error <#D6D6D6>This command is only available for players."));
      return null;
   }
}
