package de.maximanu.lobbySystem.config;

import net.kyori.adventure.text.Component;

/** One line of a {@code show-text} hotbar item's chat message. */
public record ChatLineConfig(Component text, ChatClickType click, String value, Component hover) {
}
