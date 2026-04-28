package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.util.ItemFactory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HotbarService {
   private final LobbySystem plugin;
   private final MessageService messageService;
   private final NamespacedKey hotbarKey;
   private Component infoName;
   private List<Component> infoLore;
   private Component selectorName;
   private List<Component> selectorLore;
   private Component hiderNameShown;
   private Component hiderNameOps;
   private Component hiderNameHidden;
   private List<Component> hiderLoreShown;
   private List<Component> hiderLoreOps;
   private List<Component> hiderLoreHidden;
   private int infoSlot;
   private int selectorSlot;
   private int hiderSlot;
   private Material infoMaterial;
   private Material selectorMaterial;
   private Material hiderMaterial;
   private boolean infoEnabled;
   private boolean selectorEnabled;
   private boolean hiderEnabled;
   private List<Integer> hotbarSlots;

   public HotbarService(LobbySystem plugin) {
      this.plugin = plugin;
      this.messageService = plugin.getMessageService();
      this.hotbarKey = new NamespacedKey(plugin, "hotbar_item");
      this.reload();
   }

   // Cached item templates
   public void reload() {
      this.infoName = this.messageService.component("hotbar.info.name", "<gradient:#F5F7FA:#D7DDE8>Lobby Info");
      this.infoLore = this.messageService.componentList("hotbar.info.lore", List.of("<#6E6E6E>- <#E8E8E8>View links and useful information"));
      this.selectorName = this.messageService.component("hotbar.selector.name", "<gradient:#FFE082:#FFB347>Server Selector");
      this.selectorLore = this.messageService.componentList("hotbar.selector.lore", List.of("<#6E6E6E>- <#E8E8E8>Choose where you want to play"));
      this.hiderNameShown = this.messageService.component("hotbar.hider.name.shown", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(All)");
      this.hiderNameOps = this.messageService.component("hotbar.hider.name.ops", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(Staff Only)");
      this.hiderNameHidden = this.messageService.component("hotbar.hider.name.hidden", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(Hidden)");
      this.hiderLoreShown = this.messageService.componentList("hotbar.hider.lore.shown", List.of("<#6E6E6E>- <#E8E8E8>Hide regular players"));
      this.hiderLoreOps = this.messageService.componentList("hotbar.hider.lore.ops", List.of("<#6E6E6E>- <#E8E8E8>Show staff only"));
      this.hiderLoreHidden = this.messageService.componentList("hotbar.hider.lore.hidden", List.of("<#6E6E6E>- <#E8E8E8>Show all players again"));
      this.infoSlot = this.plugin.getConfigService().getHotbarSlot("info", 0);
      this.selectorSlot = this.plugin.getConfigService().getHotbarSlot("selector", 4);
      this.hiderSlot = this.plugin.getConfigService().getHotbarSlot("hider", 8);
      this.infoMaterial = this.plugin.getConfigService().getHotbarMaterial("info", Material.BOOK);
      this.selectorMaterial = this.plugin.getConfigService().getHotbarMaterial("selector", Material.COMPASS);
      this.hiderMaterial = this.plugin.getConfigService().getHotbarMaterial("hider", Material.PLAYER_HEAD);
      this.infoEnabled = this.plugin.getConfigService().isHotbarInfoEnabled() && this.plugin.getConfigService().isLinksEnabled();
      this.selectorEnabled = this.plugin.getConfigService().isHotbarSelectorEnabled() && this.plugin.getConfigService().isSelectorMenuEnabled();
      this.hiderEnabled = this.plugin.getConfigService().isHotbarHiderEnabled();

      List<Integer> configuredSlots = new ArrayList<>();
      if (this.infoEnabled) {
         configuredSlots.add(this.infoSlot);
      }

      if (this.selectorEnabled) {
         configuredSlots.add(this.selectorSlot);
      }

      if (this.hiderEnabled) {
         configuredSlots.add(this.hiderSlot);
      }

      this.hotbarSlots = List.copyOf(configuredSlots);
   }

   // Player inventory operations
   public void giveHotbarItems(Player player) {
      this.removeHotbarItems(player);

      if (this.infoEnabled) {
         this.setHotbarItem(player, this.infoSlot, this.tagItem(ItemFactory.createNamedItem(this.infoMaterial, this.infoName, this.infoLore), "info"));
      }

      if (this.selectorEnabled) {
         this.setHotbarItem(player, this.selectorSlot, this.tagItem(ItemFactory.createNamedItem(this.selectorMaterial, this.selectorName, this.selectorLore), "selector"));
      }

      if (this.hiderEnabled) {
         this.updatePlayerHiderItem(player);
      }
   }

   public void updatePlayerHiderItem(Player player) {
      if (!this.hiderEnabled) {
         return;
      }

      PlayerVisibilityState state = this.plugin.getPlayerStateService().getPlayerVisibilityState(player.getUniqueId());
      Component name = switch(state) {
      case STAFF_ONLY -> this.hiderNameOps;
      case HIDDEN -> this.hiderNameHidden;
      case ALL -> this.hiderNameShown;
      };
      List<Component> lore = switch(state) {
      case STAFF_ONLY -> this.hiderLoreOps;
      case HIDDEN -> this.hiderLoreHidden;
      case ALL -> this.hiderLoreShown;
      };
      this.setHotbarItem(player, this.hiderSlot, this.tagItem(ItemFactory.createNamedItem(this.hiderMaterial, name, lore), "hider"));
   }

   public void removeHotbarItems(Player player) {
      for(int slot = 0; slot < player.getInventory().getSize(); ++slot) {
         ItemStack item = player.getInventory().getItem(slot);
         if (this.isTaggedHotbarItem(item)) {
            player.getInventory().setItem(slot, null);
         }
      }
   }

   public void resetLobbyInventory(Player player) {
      player.getInventory().clear();
      player.getInventory().setArmorContents(new ItemStack[4]);
      player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
      player.getInventory().setHeldItemSlot(0);
      player.updateInventory();
   }

   // Locking and lookup helpers
   public boolean isHotbarItem(ItemStack item) {
      return this.getHotbarType(item) != null;
   }

   public String getHotbarType(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return null;
      }

      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return null;
      }

      return meta.getPersistentDataContainer().get(this.hotbarKey, PersistentDataType.STRING);
   }

   public boolean isHotbarSlot(int slot) {
      return this.hotbarSlots.contains(slot);
   }

   public List<Integer> getHotbarSlots() {
      return this.hotbarSlots;
   }

   private void setHotbarItem(Player player, int slot, ItemStack item) {
      if (slot >= 0 && slot <= 8) {
         player.getInventory().setItem(slot, item);
      }
   }

   private boolean isTaggedHotbarItem(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return false;
      }

      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.getPersistentDataContainer().has(this.hotbarKey, PersistentDataType.STRING);
   }

   private ItemStack tagItem(ItemStack item, String type) {
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      meta.getPersistentDataContainer().set(this.hotbarKey, PersistentDataType.STRING, type);
      item.setItemMeta(meta);
      return item;
   }
}
