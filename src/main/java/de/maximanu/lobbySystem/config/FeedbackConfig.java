package de.maximanu.lobbySystem.config;

public record FeedbackConfig(
   FeedbackChannel spawnSet,
   FeedbackChannel spawnTeleport,
   FeedbackChannel buildMode,
   FeedbackChannel visibilityToggle,
   FeedbackChannel selectorConnect
) {
}
