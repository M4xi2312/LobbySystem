package de.maximanu.lobbySystem.config;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public record ServerEntry(Component displayName, String bungeeName, Material material, List<Component> lore, int slot) {
}
