package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.service.CosmeticService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Gadget item usage and per-player cosmetic cleanup on quit. */
public class CosmeticsListener implements Listener {
   private final CosmeticService cosmeticService;
   private final LobbyWorldService lobbyWorldService;

   public CosmeticsListener(LobbySystem plugin) {
      this.cosmeticService = plugin.getCosmeticService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
   }

   // Must run regardless of prior cancellation, like HotbarListener.onInteract - the lobby's
   // interact-protection (protection.interact) cancels PlayerInteractEvent at HIGHEST priority by
   // default, which would otherwise block gadget usage entirely before this handler ever saw it.
   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.HAND) {
         return;
      }

      Action action = event.getAction();
      if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
         return;
      }

      Player player = event.getPlayer();
      if (!this.lobbyWorldService.isLobbyWorld(player)) {
         return;
      }

      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String gadgetId = this.cosmeticService.getGadgetId(mainHand);
      if (gadgetId != null) {
         this.cosmeticService.useGadget(player, gadgetId);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.cosmeticService.handleQuit(event.getPlayer());
   }
}
