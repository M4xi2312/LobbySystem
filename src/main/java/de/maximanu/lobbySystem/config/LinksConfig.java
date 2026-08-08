package de.maximanu.lobbySystem.config;

import java.util.Map;

public record LinksConfig(boolean enabled, Map<String, String> entries) {

   public String get(String key, String fallback) {
      return this.entries.getOrDefault(key, fallback);
   }
}
