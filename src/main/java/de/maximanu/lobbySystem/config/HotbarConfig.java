package de.maximanu.lobbySystem.config;

import java.util.List;

public record HotbarConfig(boolean enabled, boolean lockItems, List<HotbarItemConfig> items) {

   /** True if at least one enabled item uses the given action (e.g. is the player-hider feature active at all). */
   public boolean isActionEnabled(HotbarAction action) {
      return this.items.stream().anyMatch((item) -> item.enabled() && item.action() == action);
   }
}
