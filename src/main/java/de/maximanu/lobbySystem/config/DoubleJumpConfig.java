package de.maximanu.lobbySystem.config;

public record DoubleJumpConfig(boolean enabled, double forwardBoost, double upwardBoost, int cooldownTicks, boolean xpBarCooldown) {
}
