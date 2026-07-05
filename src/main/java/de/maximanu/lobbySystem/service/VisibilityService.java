package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import org.bukkit.entity.Player;

public class VisibilityService {
   private final LobbySystem plugin;

   public VisibilityService(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public void applyVisibility(Player viewer, PlayerVisibilityState state) {
      for (Player target : this.plugin.getServer().getOnlinePlayers()) {
         if (!target.equals(viewer)) {
            this.applyVisibilityToTarget(viewer, target, state);
         }
      }
   }

   public void refreshPlayer(Player player) {
      PlayerVisibilityState playerState = this.plugin.getPlayerStateService().getPlayerVisibilityState(player.getUniqueId());
      for (Player other : this.plugin.getServer().getOnlinePlayers()) {
         if (other.equals(player)) {
            continue;
         }

         PlayerVisibilityState otherState = this.plugin.getPlayerStateService().getPlayerVisibilityState(other.getUniqueId());
         this.applyVisibilityToTarget(player, other, playerState);
         this.applyVisibilityToTarget(other, player, otherState);
      }
   }

   public void applyVisibilityToTarget(Player viewer, Player target, PlayerVisibilityState state) {
      if (viewer.equals(target)) {
         return;
      }

      viewer.getScheduler().execute(this.plugin, () -> {
         if (!viewer.isOnline() || !target.isOnline()) {
            return;
         }

         if (!this.shouldApplyVisibility(viewer, target)) {
            viewer.showPlayer(this.plugin, target);
            return;
         }

         switch(state) {
         case ALL -> viewer.showPlayer(this.plugin, target);
         case STAFF_ONLY -> {
            if (target.isOp()) {
               viewer.showPlayer(this.plugin, target);
            } else {
               viewer.hidePlayer(this.plugin, target);
            }
         }
         case HIDDEN -> viewer.hidePlayer(this.plugin, target);
         }
      }, null, 1L);
   }

   private boolean shouldApplyVisibility(Player viewer, Player target) {
      return this.plugin.getConfigService().isHotbarHiderEnabled()
         && this.plugin.getLobbyWorldService().isLobbyWorld(viewer)
         && this.plugin.getLobbyWorldService().isLobbyWorld(target);
   }
}
