package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.service.DoubleJumpService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/** Toggle-flight double jump: velocity boost, cooldown, and the on-ground re-arm check. */
public class DoubleJumpListener implements Listener {
   private static final long MOVE_CHECK_INTERVAL_MILLIS = 100L;

   private final ConfigService configService;
   private final LobbyPlayerService lobbyPlayerService;
   private final DoubleJumpService doubleJumpService;
   private final Map<UUID, Long> nextMoveChecks = new ConcurrentHashMap<>();

   public DoubleJumpListener(LobbySystem plugin) {
      this.configService = plugin.getConfigService();
      this.lobbyPlayerService = plugin.getLobbyPlayerService();
      this.doubleJumpService = plugin.getDoubleJumpService();
   }

   @EventHandler(ignoreCancelled = true)
   public void onToggleFlight(PlayerToggleFlightEvent event) {
      Player player = event.getPlayer();
      if (!this.lobbyPlayerService.shouldDoubleJump(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
         return;
      }

      event.setCancelled(true);
      if (this.doubleJumpService.tryUse(player)) {
         player.setAllowFlight(false);
         Vector velocity = player.getLocation().getDirection().multiply(this.configService.get().doubleJump().forwardBoost());
         velocity.setY(this.configService.get().doubleJump().upwardBoost());
         player.setVelocity(velocity);
         this.lobbyPlayerService.playSound(player, this.configService.get().sounds().doubleJump());
      }
   }

   // See LobbyPlayerService.updateDoubleJumpState for why relying on the deprecated,
   // client-reported isOnGround() here is acceptable for this feature.
   @SuppressWarnings("deprecation")
   @EventHandler(ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
         return;
      }

      if (this.lobbyPlayerService.isBuildMode(player) || player.getAllowFlight() || !player.isOnGround() || !this.lobbyPlayerService.shouldDoubleJump(player)) {
         return;
      }

      if (!this.shouldRunMoveCheck(player)) {
         return;
      }

      if (!this.doubleJumpService.isOnCooldown(player)) {
         player.setAllowFlight(true);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.doubleJumpService.clear(player, true);
      this.nextMoveChecks.remove(player.getUniqueId());
   }

   private boolean shouldRunMoveCheck(Player player) {
      long now = System.currentTimeMillis();
      long nextAllowed = this.nextMoveChecks.getOrDefault(player.getUniqueId(), 0L);
      if (nextAllowed > now) {
         return false;
      }

      this.nextMoveChecks.put(player.getUniqueId(), now + MOVE_CHECK_INTERVAL_MILLIS);
      return true;
   }
}
