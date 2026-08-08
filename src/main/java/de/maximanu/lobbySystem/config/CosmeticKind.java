package de.maximanu.lobbySystem.config;

import java.util.Locale;
import java.util.logging.Logger;

/** What a cosmetic does: worn on the head, an ambient particle trail, or a usable gadget item. */
public enum CosmeticKind {
   HAT("hat"),
   PARTICLE("particle"),
   GADGET("gadget");

   private final String configValue;

   CosmeticKind(String configValue) {
      this.configValue = configValue;
   }

   public static CosmeticKind fromConfig(String rawValue, String itemId, Logger logger) {
      if (rawValue == null || rawValue.isBlank()) {
         logger.warning("Cosmetic '" + itemId + "' has no kind configured. Disabling it.");
         return null;
      }

      String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
      for (CosmeticKind kind : values()) {
         if (kind.configValue.equals(normalized)) {
            return kind;
         }
      }

      logger.warning("Cosmetic '" + itemId + "' has unknown kind '" + rawValue + "'. Disabling it.");
      return null;
   }
}
