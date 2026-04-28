package de.maximanu.lobbySystem.config;

import java.util.Locale;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public enum FeedbackChannel {
   CHAT("chat"),
   ACTION_BAR("actionbar"),
   NONE("none");

   private final String configValue;

   FeedbackChannel(String configValue) {
      this.configValue = configValue;
   }

   public void send(Player player, Component message) {
      this.send(player, message, message);
   }

   public void send(Player player, Component chatMessage, Component actionBarMessage) {
      if (player == null) {
         return;
      }

      switch(this) {
      case CHAT -> {
         if (chatMessage != null) {
            player.sendMessage(chatMessage);
         }
      }
      case ACTION_BAR -> {
         Component message = actionBarMessage != null ? actionBarMessage : chatMessage;
         if (message != null) {
            player.sendActionBar(message);
         }
      }
      case NONE -> {
      }
      }
   }

   public String getConfigValue() {
      return this.configValue;
   }

   public static FeedbackChannel fromConfig(String rawValue, FeedbackChannel fallback, Logger logger, String path) {
      if (rawValue == null || rawValue.isBlank()) {
         return fallback;
      }

      String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
      return switch(normalized) {
      case "chat" -> CHAT;
      case "actionbar", "bar" -> ACTION_BAR;
      case "none", "off", "disabled" -> NONE;
      default -> {
         if (logger != null) {
            logger.warning("Invalid feedback channel '" + rawValue + "' at " + path + ". Using " + fallback.getConfigValue() + " instead.");
         }

         yield fallback;
      }
      };
   }
}
