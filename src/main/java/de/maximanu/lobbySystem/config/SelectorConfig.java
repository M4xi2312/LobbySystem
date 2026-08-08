package de.maximanu.lobbySystem.config;

import java.util.List;
import org.bukkit.Material;

public record SelectorConfig(
   boolean enabled,
   int size,
   List<Integer> layoutSlots,
   Material fillerMaterial,
   int previousPageSlot,
   int nextPageSlot,
   boolean fillEmptySlots,
   List<ServerEntry> servers
) {
}
