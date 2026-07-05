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
   private String managedWorldName;
   private Boolean originalAdvanceTime;
   private Boolean originalAdvanceWeather;

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

      this.restoreManagedGameRules();
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
         this.captureGameRules(world);
         this.setGameRule(world, GameRules.ADVANCE_TIME, !timeLockEnabled);
         this.setGameRule(world, GameRules.ADVANCE_WEATHER, !weatherLockEnabled);
      } else {
         this.restoreManagedGameRules();
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

   private void captureGameRules(World world) {
      if (!world.getName().equals(this.managedWorldName)) {
         this.restoreManagedGameRules();
         this.managedWorldName = world.getName();
         this.originalAdvanceTime = world.getGameRuleValue(GameRules.ADVANCE_TIME);
         this.originalAdvanceWeather = world.getGameRuleValue(GameRules.ADVANCE_WEATHER);
      }
   }

   private void restoreManagedGameRules() {
      if (this.managedWorldName == null) {
         return;
      }

      World world = this.plugin.getServer().getWorld(this.managedWorldName);
      if (world != null) {
         if (this.originalAdvanceTime != null) {
            world.setGameRule(GameRules.ADVANCE_TIME, this.originalAdvanceTime);
         }

         if (this.originalAdvanceWeather != null) {
            world.setGameRule(GameRules.ADVANCE_WEATHER, this.originalAdvanceWeather);
         }
      }

      this.managedWorldName = null;
      this.originalAdvanceTime = null;
      this.originalAdvanceWeather = null;
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
