package de.maximanu.lobbySystem.config;

public record SoundsConfig(
   boolean enabled,
   SoundEffect doubleJump,
   SoundEffect doubleJumpDeny,
   SoundEffect selectorOpen,
   SoundEffect info,
   SoundEffect hiderToggle,
   SoundEffect teleport,
   SoundEffect buildModeEnable,
   SoundEffect buildModeDisable
) {
}
