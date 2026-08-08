package de.maximanu.lobbySystem.config;

import java.util.Locale;
import java.util.logging.Logger;

/** What clicking a {@code show-text} chat line does. */
public enum ChatClickType {
   NONE("none"),
   OPEN_URL("open-url"),
   RUN_COMMAND("run-command"),
   SUGGEST_COMMAND("suggest-command"),
   COPY_TO_CLIPBOARD("copy");

   private final String configValue;

   ChatClickType(String configValue) {
      this.configValue = configValue;
   }

   public static ChatClickType fromConfig(String rawValue, String itemId, Logger logger) {
      if (rawValue == null || rawValue.isBlank()) {
         return NONE;
      }

      String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('_', '-');
      for (ChatClickType type : values()) {
         if (type.configValue.equals(normalized)) {
            return type;
         }
      }

      logger.warning("Hotbar item '" + itemId + "' has an unknown chat line click type '" + rawValue + "'. Using none.");
      return NONE;
   }
}
