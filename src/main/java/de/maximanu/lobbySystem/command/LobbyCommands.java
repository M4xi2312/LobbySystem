package de.maximanu.lobbySystem.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.maximanu.lobbySystem.LobbySystem;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/** Registers /spawn, /setspawn, /build and /lobbysystem via Paper's Brigadier command API. */
public final class LobbyCommands {
   private LobbyCommands() {
   }

   public static void register(LobbySystem plugin, Commands commands) {
      commands.register(spawnCommand(plugin), "Teleport to the lobby spawn");
      commands.register(setSpawnCommand(plugin), "Set the lobby spawn at your current position");
      commands.register(buildCommand(plugin), "Toggle build mode in the lobby");
      commands.register(lobbySystemCommand(plugin), "LobbySystem admin commands");
   }

   private static LiteralCommandNode<CommandSourceStack> spawnCommand(LobbySystem plugin) {
      return Commands.literal("spawn")
         .executes((ctx) -> {
            Player player = CommandSupport.requirePlayer(plugin, ctx.getSource());
            if (player == null) {
               return Command.SINGLE_SUCCESS;
            }

            if (!plugin.getConfigService().get().spawn().enabled()) {
               plugin.getLobbyPlayerService().sendFeatureDisabled(player, "spawn");
               return Command.SINGLE_SUCCESS;
            }

            plugin.getLobbyPlayerService().teleportToSpawnIfSet(player);
            return Command.SINGLE_SUCCESS;
         })
         .build();
   }

   private static LiteralCommandNode<CommandSourceStack> setSpawnCommand(LobbySystem plugin) {
      return Commands.literal("setspawn")
         .requires((source) -> source.getSender().hasPermission("lobbysystem.set"))
         .executes((ctx) -> {
            Player player = CommandSupport.requirePlayer(plugin, ctx.getSource());
            if (player == null) {
               return Command.SINGLE_SUCCESS;
            }

            if (!plugin.getConfigService().get().spawn().enabled()) {
               plugin.getLobbyPlayerService().sendFeatureDisabled(player, "setspawn");
               return Command.SINGLE_SUCCESS;
            }

            if (!plugin.getLobbyWorldService().isLobbyWorld(player)) {
               plugin.getLobbyWorldService().sendLobbyWorldOnlyMessage(player);
               return Command.SINGLE_SUCCESS;
            }

            plugin.getSpawnService().saveSpawnLocation(player.getLocation());
            plugin.getConfigService().get().feedback().spawnSet().send(
               player,
               plugin.getMessageService().component("info.spawn-set", "<gradient:#7DFF9C:#B8FFCC>Spawn saved</gradient> <#D6D6D6>The lobby location has been updated."),
               plugin.getMessageService().component("actionbar.spawn.set", "<gradient:#7DFF9C:#B8FFCC>Spawn saved")
            );
            return Command.SINGLE_SUCCESS;
         })
         .build();
   }

   private static LiteralCommandNode<CommandSourceStack> buildCommand(LobbySystem plugin) {
      return Commands.literal("build")
         .requires((source) -> source.getSender().hasPermission("lobbysystem.build"))
         .executes((ctx) -> {
            Player player = CommandSupport.requirePlayer(plugin, ctx.getSource());
            if (player == null) {
               return Command.SINGLE_SUCCESS;
            }

            if (!plugin.getConfigService().get().buildMode().enabled()) {
               plugin.getLobbyPlayerService().sendFeatureDisabled(player, "build mode");
               return Command.SINGLE_SUCCESS;
            }

            if (!plugin.getLobbyWorldService().isLobbyWorld(player)) {
               plugin.getLobbyWorldService().sendLobbyWorldOnlyMessage(player);
               return Command.SINGLE_SUCCESS;
            }

            plugin.getBuildModeService().toggle(player);
            plugin.getLobbyPlayerService().refreshPlayer(player);
            return Command.SINGLE_SUCCESS;
         })
         .build();
   }

   private static LiteralCommandNode<CommandSourceStack> lobbySystemCommand(LobbySystem plugin) {
      return Commands.literal("lobbysystem")
         .executes((ctx) -> {
            ctx.getSource().getSender().sendMessage(plugin.getMessageService().component("usage.lobbysystem", "Usage: /lobbysystem reload"));
            return Command.SINGLE_SUCCESS;
         })
         .then(
            Commands.literal("reload")
               .requires((source) -> source.getSender().hasPermission("lobbysystem.reload"))
               .executes((ctx) -> {
                  plugin.reloadPluginConfig();
                  plugin.getLobbyPlayerService().refreshAllPlayers();
                  ctx.getSource().getSender().sendMessage(plugin.getMessageService().component("info.config-reloaded", "<gradient:#7EE8FA:#80FFDB>Configuration reloaded!</gradient> <#D6D6D6>All values were refreshed."));
                  return Command.SINGLE_SUCCESS;
               })
         )
         .build();
   }
}
