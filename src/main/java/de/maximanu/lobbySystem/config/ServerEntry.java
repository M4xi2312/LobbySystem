package de.maximanu.lobbySystem.config;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public final class ServerEntry {
   private final Component displayName;
   private final String bungeeName;
   private final Material material;
   private final List<Component> lore;
   private final int slot;

   public ServerEntry(Component displayName, String bungeeName, Material material, List<Component> lore, int slot) {
      this.displayName = displayName;
      this.bungeeName = bungeeName;
      this.material = material;
      this.lore = lore;
      this.slot = slot;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public String getBungeeName() {
      return this.bungeeName;
   }

   public Material getMaterial() {
      return this.material;
   }

   public List<Component> getLore() {
      return this.lore;
   }

   public int getSlot() {
      return this.slot;
   }
}
