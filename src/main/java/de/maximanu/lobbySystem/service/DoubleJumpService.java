package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class DoubleJumpService {
   private static final long FEEDBACK_INTERVAL_TICKS = 2L;
   private final LobbySystem plugin;
   private final Map<UUID, Long> cooldownEndTimes = new ConcurrentHashMap<>();
   private final Map<UUID, ScheduledTask> cooldownTasks = new ConcurrentHashMap<>();
   private final Map<UUID, ExperienceSnapshot> experienceSnapshots = new ConcurrentHashMap<>();

   public DoubleJumpService(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public boolean isOnCooldown(Player player) {
      return this.getRemainingMillis(player.getUniqueId()) > 0L;
   }

   public boolean tryUse(Player player) {
      if (this.isOnCooldown(player)) {
         this.plugin.getConfigService().playSound(
            player,
            this.plugin.getConfigService().getSoundDoubleJumpDeny(),
            this.plugin.getConfigService().getSoundDoubleJumpDenyVolume(),
            this.plugin.getConfigService().getSoundDoubleJumpDenyPitch()
         );
         return false;
      }

      this.startCooldown(player);
      return true;
   }

   public void startCooldown(Player player) {
      int cooldownTicks = this.plugin.getConfigService().getDoubleJumpCooldownTicks();
      if (cooldownTicks <= 0) {
         this.restoreExperienceBar(player);
         return;
      }

      this.cooldownEndTimes.put(player.getUniqueId(), System.currentTimeMillis() + cooldownTicks * 50L);
      this.restartTask(player);
      this.updateFeedback(player);
   }

   public void refresh(Player player, boolean doubleJumpActive) {
      if (doubleJumpActive && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
         if (this.isOnCooldown(player)) {
            this.restartTask(player);
            this.updateFeedback(player);
         } else {
            this.restoreExperienceBar(player);
         }

         return;
      }

      this.clear(player, true);
   }

   public void clear(Player player, boolean resetXpBar) {
      UUID uniqueId = player.getUniqueId();
      ScheduledTask task = this.cooldownTasks.remove(uniqueId);
      if (task != null) {
         task.cancel();
      }

      this.cooldownEndTimes.remove(uniqueId);
      if (resetXpBar) {
         this.restoreExperienceBar(player);
      }
   }

   public void clear(UUID uuid) {
      ScheduledTask task = this.cooldownTasks.remove(uuid);
      if (task != null) {
         task.cancel();
      }

      this.cooldownEndTimes.remove(uuid);
      this.experienceSnapshots.remove(uuid);
   }

   // Cooldown runtime
   private void restartTask(Player player) {
      UUID uniqueId = player.getUniqueId();
      ScheduledTask previousTask = this.cooldownTasks.remove(uniqueId);
      if (previousTask != null) {
         previousTask.cancel();
      }

      ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, (scheduledTask) -> {
         if (!player.isOnline()) {
            this.clear(uniqueId);
            scheduledTask.cancel();
            return;
         }

         long remainingMillis = this.getRemainingMillis(uniqueId);
         if (remainingMillis <= 0L) {
            this.cooldownEndTimes.remove(uniqueId);
            this.cooldownTasks.remove(uniqueId);
            this.restoreExperienceBar(player);
            scheduledTask.cancel();
            return;
         }

         this.updateFeedback(player);
      }, () -> this.cooldownTasks.remove(uniqueId), 1L, FEEDBACK_INTERVAL_TICKS);
      this.cooldownTasks.put(uniqueId, task);
   }

   private void updateFeedback(Player player) {
      long remainingMillis = this.getRemainingMillis(player.getUniqueId());
      if (remainingMillis <= 0L) {
         this.restoreExperienceBar(player);
         return;
      }

      if (this.plugin.getConfigService().isDoubleJumpUseXpBar()) {
         this.captureExperienceBar(player);
         float totalMillis = Math.max(1.0F, this.plugin.getConfigService().getDoubleJumpCooldownTicks() * 50.0F);
         float progress = Math.max(0.0F, Math.min(1.0F, remainingMillis / totalMillis));
         player.setExp(progress);
         player.setLevel(Math.max(0, (int)Math.ceil(remainingMillis / 1000.0D)));
      }
   }

   // Experience bar preservation
   private void captureExperienceBar(Player player) {
      this.experienceSnapshots.computeIfAbsent(player.getUniqueId(), (uuid) -> ExperienceSnapshot.capture(player));
   }

   private void restoreExperienceBar(Player player) {
      ExperienceSnapshot snapshot = this.experienceSnapshots.remove(player.getUniqueId());
      if (snapshot != null) {
         snapshot.apply(player);
      }
   }

   private long getRemainingMillis(UUID uuid) {
      return Math.max(0L, this.cooldownEndTimes.getOrDefault(uuid, 0L) - System.currentTimeMillis());
   }

   private record ExperienceSnapshot(int totalExperience, int level, float progress) {
      private static ExperienceSnapshot capture(Player player) {
         return new ExperienceSnapshot(player.getTotalExperience(), player.getLevel(), player.getExp());
      }

      private void apply(Player player) {
         player.setTotalExperience(this.totalExperience);
         player.setLevel(this.level);
         player.setExp(this.progress);
      }
   }
}
