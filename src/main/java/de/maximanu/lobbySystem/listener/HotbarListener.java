package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ChatClickType;
import de.maximanu.lobbySystem.config.ChatLineConfig;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.event.LobbyItemUseEvent;
import de.maximanu.lobbySystem.menu.CosmeticsMenu;
import de.maximanu.lobbySystem.service.HotbarService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/** Right-click actions for the info/selector/hider hotbar items, and locking them in place. */
public class HotbarListener implements Listener {
   private final ConfigService configService;
   private final LobbyPlayerService lobbyPlayerService;
   private final LobbyWorldService lobbyWorldService;
   private final HotbarService hotbarService;
   private final CosmeticsMenu cosmeticsMenu;

   public HotbarListener(LobbySystem plugin) {
      this.configService = plugin.getConfigService();
      this.lobbyPlayerService = plugin.getLobbyPlayerService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
      this.hotbarService = plugin.getHotbarService();
      this.cosmeticsMenu = plugin.getCosmeticsMenu();
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.HAND) {
         return;
      }

      Player player = event.getPlayer();
      if (!this.configService.get().hotbar().enabled() || !this.lobbyWorldService.isLobbyWorld(player)) {
         return;
      }

      Action action = event.getAction();
      if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
         return;
      }

      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String hotbarId = this.hotbarService.getHotbarType(mainHand);
      if (hotbarId == null) {
         return;
      }

      this.hotbarService.findById(hotbarId).ifPresent((item) -> {
         LobbyItemUseEvent useEvent = new LobbyItemUseEvent(player, item.id(), item.action(), item.value());
         Bukkit.getPluginManager().callEvent(useEvent);
         if (useEvent.isCancelled()) {
            return;
         }

         switch (item.action()) {
            case LINKS -> {
               this.lobbyPlayerService.sendLinks(player);
               this.lobbyPlayerService.playSound(player, this.configService.get().sounds().info());
            }
            case SERVER_SELECTOR -> {
               this.lobbyPlayerService.getServerSelectorMenu().open(player);
               this.lobbyPlayerService.playSound(player, this.configService.get().sounds().selectorOpen());
            }
            case PLAYER_HIDER -> {
               this.lobbyPlayerService.togglePlayerHider(player);
               this.lobbyPlayerService.playSound(player, this.configService.get().sounds().hiderToggle());
            }
            case RUN_COMMAND -> {
               if (item.value() != null && !item.value().isBlank()) {
                  player.performCommand(item.value());
               }
            }
            case RUN_SCRIPT -> {
               // No built-in behavior - a script (e.g. Skript) listening to LobbyItemUseEvent handles this.
            }
            case SHOW_TEXT -> this.sendChatMessage(player, item.message());
            case COSMETICS_MENU -> this.cosmeticsMenu.open(player);
         }
      });
   }

   private void sendChatMessage(Player player, List<ChatLineConfig> lines) {
      TextReplacementConfig playerPlaceholder = TextReplacementConfig.builder().matchLiteral("{player}").replacement(player.getName()).build();
      for (ChatLineConfig line : lines) {
         Component text = line.text().replaceText(playerPlaceholder);
         if (line.hover() != null) {
            text = text.hoverEvent(HoverEvent.showText(line.hover().replaceText(playerPlaceholder)));
         }

         String value = line.value();
         switch (line.click()) {
            case OPEN_URL -> text = text.clickEvent(ClickEvent.openUrl(value));
            case RUN_COMMAND -> text = text.clickEvent(ClickEvent.runCommand(value.startsWith("/") ? value : "/" + value));
            case SUGGEST_COMMAND -> text = text.clickEvent(ClickEvent.suggestCommand(value));
            case COPY_TO_CLIPBOARD -> text = text.clickEvent(ClickEvent.copyToClipboard(value));
            case NONE -> {
            }
         }

         player.sendMessage(text);
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || MenuListener.isMenu(event.getView())) {
         return;
      }

      if (this.lobbyPlayerService.shouldHotbarLock(player) && this.isLockedHotbarInteraction(player, event)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || MenuListener.isMenu(event.getView())) {
         return;
      }

      if (!this.lobbyPlayerService.shouldHotbarLock(player)) {
         return;
      }

      Iterator<Integer> iterator = event.getRawSlots().iterator();
      while (iterator.hasNext()) {
         int rawSlot = iterator.next();
         if (this.isHotbarRawSlot(rawSlot, event.getView())) {
            event.setCancelled(true);
            return;
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldHotbarLock(player) && (this.hotbarService.isHotbarItem(event.getMainHandItem()) || this.hotbarService.isHotbarItem(event.getOffHandItem()))) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldHotbarLock(player) && this.hotbarService.isHotbarItem(event.getItemDrop().getItemStack())) {
         event.setCancelled(true);
      }
   }

   private boolean isLockedHotbarInteraction(Player player, InventoryClickEvent event) {
      if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory()) && this.hotbarService.isHotbarSlot(event.getSlot())) {
         return true;
      }

      if (this.hotbarService.isHotbarItem(event.getCurrentItem()) || this.hotbarService.isHotbarItem(event.getCursor())) {
         return true;
      }

      int hotbarButton = event.getHotbarButton();
      if (hotbarButton >= 0 && this.hotbarService.isHotbarSlot(hotbarButton)) {
         ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
         return this.hotbarService.isHotbarItem(hotbarItem);
      }

      return event.getClick() == ClickType.SWAP_OFFHAND && (this.hotbarService.isHotbarItem(player.getInventory().getItemInOffHand()) || this.hotbarService.isHotbarItem(event.getCurrentItem()));
   }

   private boolean isHotbarRawSlot(int rawSlot, InventoryView view) {
      int hotbarBase = view.getTopInventory().getSize() + 27;
      int hotbarSlot = rawSlot - hotbarBase;
      return hotbarSlot >= 0 && hotbarSlot <= 8 && this.hotbarService.isHotbarSlot(hotbarSlot);
   }
}
