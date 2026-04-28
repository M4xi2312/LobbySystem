package de.maximanu.lobbySystem.util;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemFactory {
   private ItemFactory() {
   }

   public static ItemStack createNamedItem(Material mat, Component display, List<Component> lore) {
      ItemStack i = new ItemStack(mat);
      ItemMeta m = i.getItemMeta();
      if (m != null) {
         m.displayName(nonItalic(display));
         m.lore(lore.stream().map(ItemFactory::nonItalic).toList());
         i.setItemMeta(m);
      }

      return i;
   }

   private static Component nonItalic(Component component) {
      return ((Component)(component == null ? Component.empty() : component)).decoration(TextDecoration.ITALIC, false);
   }
}
