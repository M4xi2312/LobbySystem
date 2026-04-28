package de.maximanu.lobbySystem.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateService {
   private final Map<UUID, PlayerVisibilityState> hiderStates = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> buildModes = new ConcurrentHashMap<>();

   public PlayerVisibilityState getPlayerVisibilityState(UUID uuid) {
      return this.hiderStates.getOrDefault(uuid, PlayerVisibilityState.ALL);
   }

   public void setPlayerVisibilityState(UUID uuid, PlayerVisibilityState state) {
      this.hiderStates.put(uuid, state);
   }

   public boolean getBuildMode(UUID uuid) {
      return this.buildModes.getOrDefault(uuid, false);
   }

   public void setBuildMode(UUID uuid, boolean enabled) {
      this.buildModes.put(uuid, enabled);
   }

   public void clearVisibility(UUID uuid) {
      this.hiderStates.remove(uuid);
   }

   public void clearBuildMode(UUID uuid) {
      this.buildModes.remove(uuid);
   }
}
