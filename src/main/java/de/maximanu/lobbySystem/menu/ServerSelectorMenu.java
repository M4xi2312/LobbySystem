package de.maximanu.lobbySystem.menu;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.config.ServerEntry;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.util.ItemFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ServerSelectorMenu {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final MessageService messageService;
   private final NamespacedKey bungeeKey;
   private final NamespacedKey actionKey;
   private ItemStack fillerItem;
   private Component titleBase;
   private Component fillerName;
   private Component prevPageName;
   private Component nextPageName;

   public ServerSelectorMenu(LobbySystem plugin, ConfigService configService) {
      this.plugin = plugin;
      this.configService = configService;
      this.messageService = plugin.getMessageService();
      this.bungeeKey = new NamespacedKey(plugin, "bungee");
      this.actionKey = new NamespacedKey(plugin, "selector_action");
      this.reloadMessages();
   }

   public void reloadMessages() {
      this.titleBase = this.nonItalic(this.messageService.component("menu.selector.title", "<gradient:#FFE082:#FFB347>Server Selector"));
      this.prevPageName = this.messageService.component("menu.selector.prev-page", "< Previous");
      this.nextPageName = this.messageService.component("menu.selector.next-page", "Next >");
      this.fillerName = this.messageService.component("menu.selector.filler-name", " ");
      this.updateFillerItem();
   }

   public boolean isSelectorView(InventoryView view) {
      return view != null && view.getTopInventory().getHolder() instanceof SelectorMenuHolder;
   }

   public void open(Player player) {
      this.open(player, 0);
   }

   public void open(Player player, int requestedPage) {
      if (!this.configService.isSelectorMenuEnabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "server selector");
         return;
      }

      List<ServerEntry> entries = this.configService.getServerEntries();
      int size = this.configService.getSelectorSize();
      int previousPageSlot = this.configService.getSelectorPrevSlot();
      int nextPageSlot = this.configService.getSelectorNextSlot();
      List<Integer> freeSlots = this.buildFreeSlots(size, previousPageSlot, nextPageSlot);
      int perPage = freeSlots.size();
      if (perPage <= 0) {
         this.plugin.getLogger().warning("Server selector has no usable slots. Check menu.selector.layout-slots in config.yml.");
         player.sendMessage(this.messageService.component("menu.selector.invalid-layout", "<gradient:#FF8A80:#FFB199>The selector layout in config.yml is invalid."));
         return;
      }

      int totalPages = Math.max(1, (entries.size() + perPage - 1) / perPage);
      int page = Math.min(Math.max(0, requestedPage), totalPages - 1);
      SelectorMenuHolder holder = new SelectorMenuHolder(page);
      Inventory inventory = Bukkit.createInventory(holder, size, this.title(page, totalPages));
      holder.setInventory(inventory);
      if (entries.isEmpty()) {
         Component name = this.messageService.component("menu.selector.no-servers.name", "<gradient:#FF8A80:#FFB199>No servers configured");
         List<Component> lore = this.messageService.componentList("menu.selector.no-servers.lore", List.of("<#6E6E6E>- <#E8E8E8>Add servers in config.yml"));
         inventory.setItem(Math.min(inventory.getSize() - 1, inventory.getSize() / 2), ItemFactory.createNamedItem(Material.BARRIER, name, lore));
         if (this.configService.isSelectorFillEmpty()) {
            this.fillEmpty(inventory);
         }

         player.openInventory(inventory);
         return;
      }

      this.updateFillerItem();
      int startIndex = page * perPage;
      List<ServerEntry> pageEntries = entries.subList(startIndex, Math.min(startIndex + perPage, entries.size()));
      Set<Integer> usedSlots = new HashSet<>();
      for (ServerEntry entry : pageEntries) {
         int slot = entry.getSlot();
         if (slot >= 0 && slot < size && slot != previousPageSlot && slot != nextPageSlot && !usedSlots.contains(slot) && freeSlots.contains(slot)) {
            this.setServerItem(inventory, slot, entry);
            usedSlots.add(slot);
            freeSlots.remove(Integer.valueOf(slot));
         }
      }

      for (ServerEntry entry : pageEntries) {
         if (usedSlots.contains(entry.getSlot()) || freeSlots.isEmpty()) {
            continue;
         }

         int slot = freeSlots.remove(0);
         this.setServerItem(inventory, slot, entry);
      }

      if (previousPageSlot >= 0 && totalPages > 1 && page > 0) {
         List<Component> lore = this.messageService.formatComponentList(
            "menu.selector.page-lore",
            List.of("<#6E6E6E>- <#E8E8E8>Page <#FFFFFF>{page}<#CFCFCF>/<#FFFFFF>{pages}"),
            Map.of("page", String.valueOf(page), "pages", String.valueOf(totalPages))
         );
         inventory.setItem(previousPageSlot, this.createControlItem(Material.ARROW, this.prevPageName, lore, "prev"));
      }

      if (nextPageSlot >= 0 && totalPages > 1 && page < totalPages - 1) {
         List<Component> lore = this.messageService.formatComponentList(
            "menu.selector.page-lore",
            List.of("<#6E6E6E>- <#E8E8E8>Page <#FFFFFF>{page}<#CFCFCF>/<#FFFFFF>{pages}"),
            Map.of("page", String.valueOf(page + 2), "pages", String.valueOf(totalPages))
         );
         inventory.setItem(nextPageSlot, this.createControlItem(Material.ARROW, this.nextPageName, lore, "next"));
      }

      if (this.configService.isSelectorFillEmpty()) {
         this.fillEmpty(inventory);
      }

      player.openInventory(inventory);
   }

   public boolean handleClick(Player player, ItemStack clicked, InventoryView view) {
      if (clicked == null) {
         return true;
      }

      ItemMeta meta = clicked.getItemMeta();
      if (meta == null) {
         return true;
      }

      String action = meta.getPersistentDataContainer().get(this.actionKey, PersistentDataType.STRING);
      if ("filler".equals(action)) {
         return true;
      }

      int page = this.getPage(view);
      if ("prev".equals(action)) {
         this.open(player, Math.max(0, page - 1));
         return true;
      }

      if ("next".equals(action)) {
         this.open(player, page + 1);
         return true;
      }

      String server = meta.getPersistentDataContainer().get(this.bungeeKey, PersistentDataType.STRING);
      if (server == null || server.isEmpty()) {
         return true;
      }

      player.closeInventory();
      this.configService.getSelectorConnectFeedbackChannel().send(
         player,
         this.messageService.formatComponent("info.selector-connecting", "<gradient:#FFE082:#FFB347>Connecting</gradient> <#D6D6D6>Sending you to <#FFFFFF>{server}", Map.of("server", server)),
         this.messageService.formatComponent("actionbar.selector-connecting", "<gradient:#FFE082:#FFB347>Connecting to <#FFFFFF>{server}", Map.of("server", server))
      );
      ByteArrayDataOutput output = ByteStreams.newDataOutput();
      output.writeUTF("Connect");
      output.writeUTF(server);
      player.sendPluginMessage(this.plugin, "BungeeCord", output.toByteArray());
      return true;
   }

   // Inventory composition
   private void setServerItem(Inventory inventory, int slot, ServerEntry entry) {
      ItemStack item = ItemFactory.createNamedItem(entry.getMaterial(), entry.getDisplayName(), entry.getLore());
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().set(this.bungeeKey, PersistentDataType.STRING, entry.getBungeeName());
         item.setItemMeta(meta);
      }

      inventory.setItem(slot, item);
   }

   private List<Integer> buildFreeSlots(int size, int previousPageSlot, int nextPageSlot) {
      List<Integer> configuredLayout = this.configService.getSelectorLayoutSlots();
      List<Integer> slots = new ArrayList<>();
      if (configuredLayout.isEmpty()) {
         for(int slot = 0; slot < size; ++slot) {
            if (slot != previousPageSlot && slot != nextPageSlot) {
               slots.add(slot);
            }
         }
      } else {
         slots.addAll(configuredLayout);
      }

      if (previousPageSlot >= 0) {
         slots.remove(Integer.valueOf(previousPageSlot));
      }

      if (nextPageSlot >= 0) {
         slots.remove(Integer.valueOf(nextPageSlot));
      }

      if (slots.isEmpty()) {
         for(int slot = 0; slot < size; ++slot) {
            if (slot != previousPageSlot && slot != nextPageSlot) {
               slots.add(slot);
            }
         }
      }

      return slots;
   }

   private void fillEmpty(Inventory inventory) {
      for(int slot = 0; slot < inventory.getSize(); ++slot) {
         if (inventory.getItem(slot) == null) {
            inventory.setItem(slot, this.fillerItem);
         }
      }
   }

   private void updateFillerItem() {
      this.fillerItem = this.createControlItem(this.configService.getSelectorFillerMaterial(), this.fillerName, List.of(), "filler");
   }

   private ItemStack createControlItem(Material material, Component name, List<Component> lore, String action) {
      ItemStack item = ItemFactory.createNamedItem(material, name, lore);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().set(this.actionKey, PersistentDataType.STRING, action);
         item.setItemMeta(meta);
      }

      return item;
   }

   private Component title(int page, int totalPages) {
      if (totalPages <= 1) {
         return this.titleBase.decoration(TextDecoration.ITALIC, false);
      }

      return this.titleBase.append(Component.text(" (" + (page + 1) + "/" + totalPages + ")").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false);
   }

   private int getPage(InventoryView view) {
      InventoryHolder holder = view.getTopInventory().getHolder();
      return holder instanceof SelectorMenuHolder selectorMenuHolder ? selectorMenuHolder.page() : 0;
   }

   private Component nonItalic(Component component) {
      return (component == null ? Component.empty() : component).decoration(TextDecoration.ITALIC, false);
   }

   private static final class SelectorMenuHolder implements InventoryHolder {
      private final int page;
      private Inventory inventory;

      private SelectorMenuHolder(int page) {
         this.page = page;
      }

      private int page() {
         return this.page;
      }

      private void setInventory(Inventory inventory) {
         this.inventory = inventory;
      }

      @Override
      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
