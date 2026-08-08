package de.maximanu.lobbySystem.menu;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.config.SelectorConfig;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ServerSelectorMenu {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final MessageService messageService;

   // Reassigned wholesale on reloadMessages() and read from arbitrary Folia region threads
   // whenever a player opens the menu, so it's held as a single volatile reference.
   private volatile MenuTemplates templates;

   public ServerSelectorMenu(LobbySystem plugin, ConfigService configService) {
      this.plugin = plugin;
      this.configService = configService;
      this.messageService = plugin.getMessageService();
      this.reloadMessages();
   }

   public void reloadMessages() {
      Component titleBase = ItemFactory.nonItalic(this.messageService.component("menu.selector.title", "<gradient:#FFE082:#FFB347>Server Selector"));
      Component prevPageName = this.messageService.component("menu.selector.prev-page", "< Previous");
      Component nextPageName = this.messageService.component("menu.selector.next-page", "Next >");
      Component fillerName = this.messageService.component("menu.selector.filler-name", " ");
      ItemStack fillerItem = this.buildControlItem(this.configService.get().selector().fillerMaterial(), fillerName, List.of());
      this.templates = new MenuTemplates(titleBase, prevPageName, nextPageName, fillerItem);
   }

   public void open(Player player) {
      this.open(player, 0);
   }

   public void open(Player player, int requestedPage) {
      MenuTemplates templates = this.templates;
      SelectorConfig selectorConfig = this.configService.get().selector();
      if (!selectorConfig.enabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "server selector");
         return;
      }

      List<ServerEntry> entries = selectorConfig.servers();
      int size = selectorConfig.size();
      int previousPageSlot = selectorConfig.previousPageSlot();
      int nextPageSlot = selectorConfig.nextPageSlot();
      List<Integer> freeSlots = this.buildFreeSlots(selectorConfig, previousPageSlot, nextPageSlot);
      int perPage = freeSlots.size();
      if (perPage <= 0) {
         this.plugin.getLogger().warning("Server selector has no usable slots. Check server-selector.menu.layout-slots in config.yml.");
         player.sendMessage(this.messageService.component("menu.selector.invalid-layout", "<gradient:#FF8A80:#FFB199>The selector layout in config.yml is invalid."));
         return;
      }

      int totalPages = Math.max(1, (entries.size() + perPage - 1) / perPage);
      int page = Math.min(Math.max(0, requestedPage), totalPages - 1);
      SelectorMenu menu = new SelectorMenu(size, this.title(templates.titleBase(), page, totalPages));

      if (entries.isEmpty()) {
         Component name = this.messageService.component("menu.selector.no-servers.name", "<gradient:#FF8A80:#FFB199>No servers configured");
         List<Component> lore = this.messageService.componentList("menu.selector.no-servers.lore", List.of("<#6E6E6E>- <#E8E8E8>Add servers in config.yml"));
         menu.set(Math.min(size - 1, size / 2), ItemFactory.createNamedItem(Material.BARRIER, name, lore));
         if (selectorConfig.fillEmptySlots()) {
            menu.fillEmpty(templates.fillerItem());
         }

         menu.open(player);
         return;
      }

      int startIndex = page * perPage;
      List<ServerEntry> pageEntries = entries.subList(startIndex, Math.min(startIndex + perPage, entries.size()));
      Set<Integer> usedSlots = new HashSet<>();
      for (ServerEntry entry : pageEntries) {
         int slot = entry.slot();
         if (slot >= 0 && slot < size && slot != previousPageSlot && slot != nextPageSlot && !usedSlots.contains(slot) && freeSlots.contains(slot)) {
            this.setServerItem(menu, slot, entry, player);
            usedSlots.add(slot);
            freeSlots.remove(Integer.valueOf(slot));
         }
      }

      for (ServerEntry entry : pageEntries) {
         if (usedSlots.contains(entry.slot()) || freeSlots.isEmpty()) {
            continue;
         }

         int slot = freeSlots.remove(0);
         this.setServerItem(menu, slot, entry, player);
      }

      if (previousPageSlot >= 0 && totalPages > 1 && page > 0) {
         List<Component> lore = this.pageLore(page, totalPages);
         menu.set(previousPageSlot, this.buildControlItem(Material.ARROW, templates.prevPageName(), lore), (event) -> this.open(player, page - 1));
      }

      if (nextPageSlot >= 0 && totalPages > 1 && page < totalPages - 1) {
         List<Component> lore = this.pageLore(page + 2, totalPages);
         menu.set(nextPageSlot, this.buildControlItem(Material.ARROW, templates.nextPageName(), lore), (event) -> this.open(player, page + 1));
      }

      if (selectorConfig.fillEmptySlots()) {
         menu.fillEmpty(templates.fillerItem());
      }

      menu.open(player);
   }

   private void setServerItem(SelectorMenu menu, int slot, ServerEntry entry, Player player) {
      ItemStack item = ItemFactory.createNamedItem(entry.material(), entry.displayName(), entry.lore());
      menu.set(slot, item, (event) -> this.connect(player, entry.bungeeName()));
   }

   private void connect(Player player, String server) {
      player.closeInventory();
      this.configService.get().feedback().selectorConnect().send(
         player,
         this.messageService.formatComponent("info.selector-connecting", "<gradient:#FFE082:#FFB347>Connecting</gradient> <#D6D6D6>Sending you to <#FFFFFF>{server}", Map.of("server", server)),
         this.messageService.formatComponent("actionbar.selector-connecting", "<gradient:#FFE082:#FFB347>Connecting to <#FFFFFF>{server}", Map.of("server", server))
      );
      ByteArrayDataOutput output = ByteStreams.newDataOutput();
      output.writeUTF("Connect");
      output.writeUTF(server);
      player.sendPluginMessage(this.plugin, "BungeeCord", output.toByteArray());
   }

   // Inventory composition
   private List<Integer> buildFreeSlots(SelectorConfig selectorConfig, int previousPageSlot, int nextPageSlot) {
      List<Integer> configuredLayout = selectorConfig.layoutSlots();
      List<Integer> slots = new ArrayList<>();
      if (configuredLayout.isEmpty()) {
         for (int slot = 0; slot < selectorConfig.size(); ++slot) {
            if (slot != previousPageSlot && slot != nextPageSlot) {
               slots.add(slot);
            }
         }
      } else {
         slots.addAll(configuredLayout);
      }

      slots.remove(Integer.valueOf(previousPageSlot));
      slots.remove(Integer.valueOf(nextPageSlot));

      if (slots.isEmpty()) {
         for (int slot = 0; slot < selectorConfig.size(); ++slot) {
            if (slot != previousPageSlot && slot != nextPageSlot) {
               slots.add(slot);
            }
         }
      }

      return slots;
   }

   private List<Component> pageLore(int displayPage, int totalPages) {
      return this.messageService.formatComponentList(
         "menu.selector.page-lore",
         List.of("<#6E6E6E>- <#E8E8E8>Page <#FFFFFF>{page}<#CFCFCF>/<#FFFFFF>{pages}"),
         Map.of("page", String.valueOf(displayPage), "pages", String.valueOf(totalPages))
      );
   }

   private ItemStack buildControlItem(Material material, Component name, List<Component> lore) {
      return ItemFactory.createNamedItem(material, name, lore);
   }

   private Component title(Component titleBase, int page, int totalPages) {
      if (totalPages <= 1) {
         return titleBase;
      }

      return ItemFactory.nonItalic(titleBase.append(Component.text(" (" + (page + 1) + "/" + totalPages + ")").color(NamedTextColor.GRAY)));
   }

   private record MenuTemplates(Component titleBase, Component prevPageName, Component nextPageName, ItemStack fillerItem) {
   }

   private static final class SelectorMenu extends AbstractMenu {
      private SelectorMenu(int size, Component title) {
         super(size, title);
      }
   }
}
