package de.maximanu.lobbySystem.config;

public record JoinConfig(boolean enabled, boolean hideJoinMessage, boolean hideQuitMessage, String customJoinMessage, String customQuitMessage) {
}
