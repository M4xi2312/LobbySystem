package de.maximanu.lobbySystem.config;

import java.util.Locale;
import java.util.logging.Logger;

/** What a {@link CosmeticKind#GADGET} does when used. */
public enum GadgetEffect {
   FIREWORK("firework"),
   GRAPPLING_HOOK("grappling-hook"),
   PEARL_BOW("pearl-bow");

   private final String configValue;

   GadgetEffect(String configValue) {
      this.configValue = configValue;
   }

   public static GadgetEffect fromConfig(String rawValue, String itemId, Logger logger) {
      if (rawValue == null || rawValue.isBlank()) {
         return FIREWORK;
      }

      String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('_', '-');
      for (GadgetEffect effect : values()) {
         if (effect.configValue.equals(normalized)) {
            return effect;
         }
      }

      logger.warning("Cosmetic '" + itemId + "' has unknown gadget effect '" + rawValue + "'. Using firework.");
      return FIREWORK;
   }
}
