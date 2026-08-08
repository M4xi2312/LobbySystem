package de.maximanu.lobbySystem.config;

import java.util.List;

public record CosmeticsConfig(boolean enabled, int menuSize, List<CosmeticConfig> items) {

   public List<CosmeticConfig> itemsOf(CosmeticKind kind) {
      return this.items.stream().filter((item) -> item.kind() == kind).toList();
   }
}
