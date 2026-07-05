package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class BuildModeService {
   private final LobbySystem plugin;
   private final PlayerStateService playerStateService;
   private final MessageService messageService;
   private final Map<UUID, Boolean> activeStates = new ConcurrentHashMap<>();

   public BuildModeService(LobbySystem plugin) {
      this.plugin = plugin;
      this.playerStateService = plugin.getPlayerStateService();
      this.messageService = plugin.getMessageService();
   }

   public boolean toggle(Player player) {
      boolean enabled = !this.isEnabled(player);
      this.setEnabled(player, enabled);
      this.applyState(player);
      this.sendFeedback(player, enabled);
      return enabled;
   }

   public boolean isEnabled(Player player) {
      return this.playerStateService.getBuildMode(player.getUniqueId());
   }

   public void setEnabled(Player player, boolean enabled) {
      this.playerStateService.setBuildMode(player.getUniqueId(), enabled);
   }

   // Runtime state
   public void applyState(Player player) {
      UUID uniqueId = player.getUniqueId();
      boolean active = this.plugin.getConfigService().isBuildModeEnabled()
         && this.plugin.getLobbyWorldService().isLobbyWorld(player)
         && this.isEnabled(player);
      boolean wasActive = this.activeStates.getOrDefault(uniqueId, false);

      if (!active) {
         this.activeStates.remove(uniqueId);
         if (wasActive && this.plugin.getLobbyWorldService().isLobbyWorld(player)) {
            this.plugin.getHotbarService().resetLobbyInventory(player);
         }

         if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
         }

         return;
      }

      this.activeStates.put(uniqueId, true);
      if (this.plugin.getConfigService().isBuildModeAllowFlight() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
         player.setAllowFlight(true);
      }
   }

   public void clearOnQuit(Player player) {
      if (this.plugin.getConfigService().isBuildModeResetOnQuit()) {
         this.playerStateService.clearBuildMode(player.getUniqueId());
      }

      this.activeStates.remove(player.getUniqueId());
   }

   // User feedback
   private void sendFeedback(Player player, boolean enabled) {
      this.plugin.getConfigService().getBuildModeFeedbackChannel().send(
         player,
         this.messageService.component(enabled ? "info.build-enabled" : "info.build-disabled", enabled
            ? "<gradient:#FFD166:#FFB347>Build mode enabled</gradient> <#D6D6D6>You can now edit the lobby."
            : "<gradient:#FF8FA3:#FFB3C1>Build mode disabled</gradient> <#D6D6D6>You are back in normal lobby mode."),
         this.messageService.component(enabled ? "actionbar.build-mode.enabled" : "actionbar.build-mode.disabled", enabled
            ? "<gradient:#FFD166:#FFB347>Build mode enabled"
            : "<gradient:#9E9E9E:#D6D6D6>Build mode disabled")
      );

      this.plugin.getConfigService().playSound(
         player,
         enabled ? this.plugin.getConfigService().getSoundBuildModeEnable() : this.plugin.getConfigService().getSoundBuildModeDisable(),
         enabled ? this.plugin.getConfigService().getSoundBuildModeEnableVolume() : this.plugin.getConfigService().getSoundBuildModeDisableVolume(),
         enabled ? this.plugin.getConfigService().getSoundBuildModeEnablePitch() : this.plugin.getConfigService().getSoundBuildModeDisablePitch()
      );
   }
}
