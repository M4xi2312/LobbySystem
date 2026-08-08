package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.config.FeedbackChannel;
import de.maximanu.lobbySystem.config.HotbarAction;
import de.maximanu.lobbySystem.config.SoundEffect;
import de.maximanu.lobbySystem.menu.ServerSelectorMenu;
import de.maximanu.lobbySystem.model.PlayerVisibilityState;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LobbyPlayerService {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final HotbarService hotbarService;
   private final ServerSelectorMenu serverSelectorMenu;
   private final MessageService messageService;
   private final PlayerStateService playerStateService;
   private final SpawnService spawnService;
   private final VisibilityService visibilityService;
   private final DoubleJumpService doubleJumpService;
   private final BuildModeService buildModeService;
   private final LobbyWorldService lobbyWorldService;
   private final CosmeticService cosmeticService;

   public LobbyPlayerService(LobbySystem plugin) {
      this.plugin = plugin;
      this.configService = plugin.getConfigService();
      this.hotbarService = plugin.getHotbarService();
      this.serverSelectorMenu = plugin.getServerSelectorMenu();
      this.messageService = plugin.getMessageService();
      this.playerStateService = plugin.getPlayerStateService();
      this.spawnService = plugin.getSpawnService();
      this.visibilityService = plugin.getVisibilityService();
      this.doubleJumpService = plugin.getDoubleJumpService();
      this.buildModeService = plugin.getBuildModeService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
      this.cosmeticService = plugin.getCosmeticService();
   }

   // Player lifecycle
   public void handleJoin(Player player) {
      player.getScheduler().execute(this.plugin, () -> {
         if (!player.isOnline()) {
            return;
         }

         if (this.configService.get().spawn().enabled() && this.configService.get().spawn().teleportOnJoin() && this.teleportToSpawnIfSet(player, false, () -> this.refreshPlayer(player))) {
            return;
         }

         this.refreshPlayer(player);
      }, null, 1L);
   }

   public void handleRespawn(Player player) {
      player.getScheduler().runDelayed(this.plugin, (task) -> this.refreshPlayer(player), null, 1L);
   }

   public void refreshAllPlayers() {
      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         player.getScheduler().execute(this.plugin, () -> this.refreshPlayer(player), null, 1L);
      }
   }

   public void refreshPlayer(Player player) {
      if (!player.isOnline()) {
         return;
      }

      this.buildModeService.applyState(player);
      this.updateHotbarState(player);
      this.visibilityService.refreshPlayer(player);
      this.doubleJumpService.refresh(player, this.shouldDoubleJump(player));
      this.updateDoubleJumpState(player);
      this.cosmeticService.reapply(player);
   }

   // Feature actions
   public void sendLinks(Player player) {
      if (!this.configService.get().links().enabled()) {
         this.sendFeatureDisabled(player, "links");
         return;
      }

      this.sendClickableLink(player, "links.website", "Website: {link}", this.configService.get().links().get("website", "https://example.com"));
      this.sendClickableLink(player, "links.discord", "Discord: {link}", this.configService.get().links().get("discord", "https://discord.gg/example"));
      this.sendClickableLink(player, "links.store", "Store: {link}", this.configService.get().links().get("store", "https://store.example.com"));
   }

   public void togglePlayerHider(Player player) {
      if (!this.configService.get().hotbar().isActionEnabled(HotbarAction.PLAYER_HIDER)) {
         this.sendFeatureDisabled(player, "player hider");
         return;
      }

      if (!this.lobbyWorldService.isLobbyWorld(player)) {
         this.lobbyWorldService.sendLobbyWorldOnlyMessage(player);
         return;
      }

      UUID uniqueId = player.getUniqueId();
      PlayerVisibilityState state = this.playerStateService.getPlayerVisibilityState(uniqueId).next();
      this.playerStateService.setPlayerVisibilityState(uniqueId, state);
      this.visibilityService.applyVisibility(player, state);
      this.hotbarService.updatePlayerHiderItem(player);

      Component chatMessage = switch (state) {
         case ALL -> this.messageService.component("info.visibility.all", "<gradient:#7EE8FA:#5AA9FF>Players</gradient> <#E8E8E8>All visible");
         case STAFF_ONLY -> this.messageService.component("info.visibility.ops", "<gradient:#7EE8FA:#5AA9FF>Players</gradient> <#E8E8E8>Staff only");
         case HIDDEN -> this.messageService.component("info.visibility.hidden", "<gradient:#7EE8FA:#5AA9FF>Players</gradient> <#E8E8E8>All hidden");
      };
      FeedbackChannel feedbackChannel = this.configService.get().feedback().visibilityToggle();
      feedbackChannel.send(player, chatMessage);
   }

   public boolean teleportToSpawnIfSet(Player player) {
      return this.teleportToSpawnIfSet(player, true, null);
   }

   public boolean teleportToSpawnIfSet(Player player, boolean sendFeedback, Runnable completion) {
      if (!this.configService.get().spawn().enabled()) {
         if (sendFeedback) {
            this.sendFeatureDisabled(player, "spawn");
         }

         if (completion != null) {
            completion.run();
         }

         return false;
      }

      Location spawn = this.spawnService.getSpawnLocation();
      if (spawn == null) {
         if (sendFeedback) {
            if (player.isOp()) {
               player.sendMessage(this.messageService.component("errors.spawn-not-set-op", "<#FF5C5C>Warning <#D6D6D6>No spawn set. <#FFFFFF>Use /setspawn."));
            } else {
               player.sendMessage(this.messageService.component("errors.spawn-not-set", "<#FF5C5C>Warning <#D6D6D6>No spawn has been configured yet."));
            }
         }

         if (completion != null) {
            completion.run();
         }

         return false;
      }

      player.teleportAsync(spawn).whenComplete((success, throwable) -> {
         if (throwable != null) {
            this.plugin.getLogger().warning("Failed to teleport " + player.getName() + " to spawn: " + throwable.getMessage());
            if (player.isOnline()) {
               player.getScheduler().execute(this.plugin, () -> {
                  if (sendFeedback) {
                     player.sendMessage(this.messageService.component("errors.teleport-failed", "<#FF5C5C>Error <#D6D6D6>Teleport failed. Please try again."));
                  }

                  if (completion != null) {
                     completion.run();
                  }
               }, null, 1L);
            }

            return;
         }

         if (Boolean.TRUE.equals(success) && player.isOnline()) {
            player.getScheduler().execute(this.plugin, () -> {
               if (sendFeedback) {
                  this.configService.get().feedback().spawnTeleport().send(
                     player,
                     this.messageService.component("info.spawn-teleport", "<gradient:#7DFF9C:#B8FFCC>Lobby</gradient> <#D6D6D6>You were teleported to spawn."),
                     this.messageService.component("actionbar.spawn.teleport", "<gradient:#7DFF9C:#B8FFCC>Teleported to spawn")
                  );
               }

               this.playSound(player, this.configService.get().sounds().teleport());
               if (completion != null) {
                  completion.run();
               }
            }, null, 1L);
         } else if (player.isOnline()) {
            player.getScheduler().execute(this.plugin, () -> {
               if (sendFeedback) {
                  player.sendMessage(this.messageService.component("errors.teleport-failed", "<#FF5C5C>Error <#D6D6D6>Teleport failed. Please try again."));
               }

               if (completion != null) {
                  completion.run();
               }
            }, null, 1L);
         }
      });
      return true;
   }

   // Availability checks
   public boolean shouldProtect(Player player) {
      return this.configService.get().protection().enabled() && this.lobbyWorldService.isLobbyWorld(player);
   }

   public boolean shouldHotbarLock(Player player) {
      return this.configService.get().hotbar().enabled() && this.configService.get().hotbar().lockItems() && this.lobbyWorldService.isLobbyWorld(player) && !this.isBuildMode(player);
   }

   public boolean shouldDoubleJump(Player player) {
      if (!this.configService.get().doubleJump().enabled() || !this.lobbyWorldService.isLobbyWorld(player)) {
         return false;
      }

      return !this.isBuildMode(player) || !this.configService.get().buildMode().disableDoubleJump();
   }

   public boolean shouldBlockCreativePick(Player player) {
      return this.lobbyWorldService.isLobbyWorld(player) && !this.isBuildMode(player);
   }

   public boolean isBuildMode(Player player) {
      return this.configService.get().buildMode().enabled() && this.buildModeService.isEnabled(player);
   }

   public ServerSelectorMenu getServerSelectorMenu() {
      return this.serverSelectorMenu;
   }

   public HotbarService getHotbarService() {
      return this.hotbarService;
   }

   public DoubleJumpService getDoubleJumpService() {
      return this.doubleJumpService;
   }

   public void playSound(Player player, SoundEffect effect) {
      this.configService.playSound(player, effect);
   }

   public void sendFeatureDisabled(Player player, String featureName) {
      player.sendMessage(this.messageService.formatComponent("errors.feature-disabled", "<#FF5C5C>Error <#D6D6D6>The feature <#FFFFFF>{feature} <#D6D6D6>is disabled in config.", Map.of("feature", featureName)));
   }

   private void sendClickableLink(Player player, String messagePath, String fallback, String link) {
      Component message = this.messageService.formatComponent(messagePath, fallback, Map.of("link", link))
         .clickEvent(ClickEvent.openUrl(link))
         .hoverEvent(HoverEvent.showText(this.messageService.formatComponent("links.hover", "<#D6D6D6>Open <#FFFFFF>{link}", Map.of("link", link))));
      player.sendMessage(message);
   }

   // Internal state sync
   private boolean shouldGiveHotbar(Player player) {
      return this.configService.get().hotbar().enabled() && this.lobbyWorldService.isLobbyWorld(player) && !this.isBuildMode(player);
   }

   private void updateHotbarState(Player player) {
      if (this.shouldGiveHotbar(player)) {
         this.hotbarService.giveHotbarItems(player);
      } else {
         this.hotbarService.removeHotbarItems(player);
      }
   }

   // Player#isOnGround() is client-reported and technically spoofable, but the actual double-jump
   // consumption is still gated server-side by DoubleJumpService's cooldown, so the worst a spoofed
   // value can do here is toggle flight-eligibility early - not bypass the cooldown itself.
   @SuppressWarnings("deprecation")
   private void updateDoubleJumpState(Player player) {
      if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
         return;
      }

      if (this.isBuildMode(player) && this.lobbyWorldService.isLobbyWorld(player)) {
         boolean allowFlight = this.configService.get().buildMode().allowFlight();
         if (player.getAllowFlight() != allowFlight) {
            player.setAllowFlight(allowFlight);
         }

         return;
      }

      boolean allowDoubleJump = this.shouldDoubleJump(player) && player.isOnGround() && !this.doubleJumpService.isOnCooldown(player);
      if (player.getAllowFlight() != allowDoubleJump) {
         player.setAllowFlight(allowDoubleJump);
      }

      if (!allowDoubleJump && player.isFlying()) {
         player.setFlying(false);
      }
   }
}
