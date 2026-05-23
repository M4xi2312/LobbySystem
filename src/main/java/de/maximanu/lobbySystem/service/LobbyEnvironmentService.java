package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.World;

public class LobbyEnvironmentService {
   private static final long INITIAL_DELAY_TICKS = 1L;
   private static final long CHECK_INTERVAL_TICKS = 600L;

   private final LobbySystem plugin;
   private ScheduledTask task;
   private volatile boolean applyingTimeLock;

   public LobbyEnvironmentService(LobbySystem plugin) {
      this.plugin = plugin;
   }

   public void start() {
      this.stop();
      this.task = this.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(this.plugin, (task) -> this.apply(), INITIAL_DELAY_TICKS, CHECK_INTERVAL_TICKS);
   }

   public void reload() {
      this.plugin.getServer().getGlobalRegionScheduler().run(this.plugin, (task) -> this.apply());
   }

   public void stop() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }

   public boolean isApplyingTimeLock() {
      return this.applyingTimeLock;
   }

   private void apply() {
      World world = this.getConfiguredLobbyWorld();
      if (world == null) {
         return;
      }

      ConfigService config = this.plugin.getConfigService();
      boolean timeLockEnabled = config.isProtectionEnabled() && config.isProtectTimeLock();
      boolean weatherLockEnabled = config.isProtectionEnabled() && config.isProtectWeatherChange();

      if (config.isProtectManageGameRules()) {
         this.setGameRule(world, GameRules.ADVANCE_TIME, !timeLockEnabled);
         this.setGameRule(world, GameRules.ADVANCE_WEATHER, !weatherLockEnabled);
      }

      if (timeLockEnabled) {
         this.syncTime(world, config.getLockedTime());
      }

      if (weatherLockEnabled) {
         this.clearWeather(world);
      }
   }

   private World getConfiguredLobbyWorld() {
      String worldName = this.plugin.getConfigService().getLobbyWorldName();
      if (worldName.isBlank()) {
         return null;
      }

      return this.plugin.getServer().getWorld(worldName);
   }

   private void syncTime(World world, long lockedTime) {
      if (world.getTime() == lockedTime) {
         return;
      }

      this.applyingTimeLock = true;
      try {
         world.setTime(lockedTime);
      } finally {
         this.applyingTimeLock = false;
      }
   }

   private void clearWeather(World world) {
      if (world.hasStorm()) {
         world.setStorm(false);
      }

      if (world.isThundering()) {
         world.setThundering(false);
      }
   }

   private void setGameRule(World world, GameRule<Boolean> gameRule, boolean value) {
      Boolean currentValue = world.getGameRuleValue(gameRule);
      if (!Boolean.valueOf(value).equals(currentValue)) {
         world.setGameRule(gameRule, value);
      }
   }
}
