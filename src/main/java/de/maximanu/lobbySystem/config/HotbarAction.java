package de.maximanu.lobbySystem.config;

import java.util.Locale;
import java.util.logging.Logger;

/** What a hotbar item does when right-clicked. Extend this to add new item behaviors. */
public enum HotbarAction {
   LINKS("links"),
   SERVER_SELECTOR("server-selector"),
   PLAYER_HIDER("player-hider"),
   RUN_COMMAND("run-command"),
   RUN_SCRIPT("run-script"),
   SHOW_TEXT("show-text"),
   COSMETICS_MENU("cosmetics-menu");

   private final String configValue;

   HotbarAction(String configValue) {
      this.configValue = configValue;
   }

   public String getConfigValue() {
      return this.configValue;
   }

   public static HotbarAction fromConfig(String rawValue, String itemId, Logger logger) {
      if (rawValue == null || rawValue.isBlank()) {
         logger.warning("Hotbar item '" + itemId + "' has no action configured. Disabling it.");
         return null;
      }

      String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('_', '-');
      for (HotbarAction action : values()) {
         if (action.configValue.equals(normalized)) {
            return action;
         }
      }

      logger.warning("Hotbar item '" + itemId + "' has unknown action '" + rawValue + "'. Disabling it.");
      return null;
   }
}
