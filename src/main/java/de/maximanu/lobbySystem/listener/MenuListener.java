package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.menu.AbstractMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Routes clicks/drags for any {@link AbstractMenu}-backed inventory to its own handler map. */
public class MenuListener implements Listener {

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (!(event.getView().getTopInventory().getHolder() instanceof AbstractMenu menu)) {
         return;
      }

      event.setCancelled(true);
      if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
         menu.handleClick(event);
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof AbstractMenu) {
         event.setCancelled(true);
      }
   }

   public static boolean isMenu(org.bukkit.inventory.InventoryView view) {
      return view != null && view.getTopInventory().getHolder() instanceof AbstractMenu;
   }
}
