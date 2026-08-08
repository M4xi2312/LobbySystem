package de.maximanu.lobbySystem.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Reusable inventory-menu base: items are placed together with the click handler that should
 * run when they're clicked, instead of encoding actions as strings in item PersistentDataContainers.
 * {@link de.maximanu.lobbySystem.listener.MenuListener} routes clicks to whichever menu is open.
 */
public abstract class AbstractMenu implements InventoryHolder {
   private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
   private Inventory inventory;

   protected AbstractMenu(int size, Component title) {
      this.inventory = Bukkit.createInventory(this, size, title);
   }

   protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
      this.inventory.setItem(slot, item);
      if (onClick != null) {
         this.clickHandlers.put(slot, onClick);
      } else {
         this.clickHandlers.remove(slot);
      }
   }

   protected void set(int slot, ItemStack item) {
      this.set(slot, item, null);
   }

   /** Fills every currently-empty slot with a non-interactive filler item. */
   protected void fillEmpty(ItemStack fillerItem) {
      for (int slot = 0; slot < this.inventory.getSize(); ++slot) {
         if (this.inventory.getItem(slot) == null) {
            this.set(slot, fillerItem);
         }
      }
   }

   public void handleClick(InventoryClickEvent event) {
      Consumer<InventoryClickEvent> handler = this.clickHandlers.get(event.getSlot());
      if (handler != null) {
         handler.accept(event);
      }
   }

   public void open(Player player) {
      player.openInventory(this.inventory);
   }

   @Override
   public Inventory getInventory() {
      return this.inventory;
   }
}
