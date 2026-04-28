package de.maximanu.lobbySystem.service;

public enum PlayerVisibilityState {
   ALL,
   STAFF_ONLY,
   HIDDEN;

   public PlayerVisibilityState next() {
      return switch(this) {
      case ALL -> STAFF_ONLY;
      case STAFF_ONLY -> HIDDEN;
      case HIDDEN -> ALL;
      };
   }
}
