package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.config.JoinConfig;
import de.maximanu.lobbySystem.service.BuildModeService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.service.PlayerStateService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

/** Join/quit/respawn/world-change bookkeeping, plus join & quit message formatting. */
public class PlayerLifecycleListener implements Listener {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final LobbyPlayerService lobbyPlayerService;
   private final LobbyWorldService lobbyWorldService;
   private final BuildModeService buildModeService;
   private final PlayerStateService playerStateService;
   private final MessageService messageService;

   public PlayerLifecycleListener(LobbySystem plugin) {
      this.plugin = plugin;
      this.configService = plugin.getConfigService();
      this.lobbyPlayerService = plugin.getLobbyPlayerService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
      this.buildModeService = plugin.getBuildModeService();
      this.playerStateService = plugin.getPlayerStateService();
      this.messageService = plugin.getMessageService();
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.applyJoinMessage(event);
      this.lobbyPlayerService.handleJoin(event.getPlayer());
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      Location spawn = this.plugin.getSpawnService().getSpawnLocation();
      if (this.configService.get().spawn().enabled() && this.configService.get().spawn().teleportOnRespawn() && spawn != null) {
         event.setRespawnLocation(spawn);
      }

      this.lobbyPlayerService.handleRespawn(event.getPlayer());
   }

   @EventHandler
   public void onWorldChange(PlayerChangedWorldEvent event) {
      this.lobbyPlayerService.refreshPlayer(event.getPlayer());
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      if (event.getWorld().getName().equalsIgnoreCase(this.configService.get().spawnWorldName())) {
         this.plugin.getSpawnService().reload();
      }

      if (this.lobbyWorldService.isLobbyWorld(event.getWorld())) {
         this.plugin.getLobbyEnvironmentService().reload();
      }
   }

   @EventHandler
   public void onGameModeChange(PlayerGameModeChangeEvent event) {
      Player player = event.getPlayer();
      player.getScheduler().execute(this.plugin, () -> this.lobbyPlayerService.refreshPlayer(player), null, 1L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.applyQuitMessage(event);
      Player player = event.getPlayer();
      this.buildModeService.clearOnQuit(player);
      this.playerStateService.clearVisibility(player.getUniqueId());
   }

   private void applyJoinMessage(PlayerJoinEvent event) {
      JoinConfig join = this.configService.get().join();
      if (!join.enabled()) {
         return;
      }

      if (join.hideJoinMessage()) {
         event.joinMessage(null);
         return;
      }

      Component message = this.messageService.deserialize(join.customJoinMessage().replace("{player}", event.getPlayer().getName()));
      event.joinMessage(message);
   }

   private void applyQuitMessage(PlayerQuitEvent event) {
      JoinConfig join = this.configService.get().join();
      if (!join.enabled()) {
         return;
      }

      if (join.hideQuitMessage()) {
         event.quitMessage(null);
         return;
      }

      Component message = this.messageService.deserialize(join.customQuitMessage().replace("{player}", event.getPlayer().getName()));
      event.quitMessage(message);
   }
}
