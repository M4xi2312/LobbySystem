package de.maximanu.lobbySystem.config;

public record PluginConfig(
   String lobbyWorldName,
   String spawnWorldName,
   SpawnConfig spawn,
   ProtectionConfig protection,
   DoubleJumpConfig doubleJump,
   BuildModeConfig buildMode,
   HotbarConfig hotbar,
   SelectorConfig selector,
   FeedbackConfig feedback,
   SoundsConfig sounds,
   LinksConfig links,
   JoinConfig join,
   CosmeticsConfig cosmetics
) {
}
