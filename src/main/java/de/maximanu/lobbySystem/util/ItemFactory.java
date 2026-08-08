package de.maximanu.lobbySystem.util;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ItemFactory {
   private ItemFactory() {
   }

   public static ItemStack createNamedItem(Material material, Component display, List<Component> lore) {
      ItemStack item = new ItemStack(material);
      item.editMeta((meta) -> {
         meta.displayName(nonItalic(display));
         meta.lore(lore.stream().map(ItemFactory::nonItalic).toList());
      });
      return item;
   }

   public static Component nonItalic(Component component) {
      return (component == null ? Component.empty() : component).decoration(TextDecoration.ITALIC, false);
   }
}
