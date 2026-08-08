package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.HotbarAction;
import de.maximanu.lobbySystem.config.HotbarItemConfig;
import de.maximanu.lobbySystem.model.PlayerVisibilityState;
import de.maximanu.lobbySystem.util.ItemFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HotbarService {
   private final LobbySystem plugin;
   private final MessageService messageService;
   private final NamespacedKey hotbarKey;

   // Reassigned wholesale on reload() and read from arbitrary Folia region threads via event
   // handlers, so both are held as single volatile references rather than separate mutable fields.
   private volatile HiderTemplates hiderTemplates;
   private volatile List<Integer> hotbarSlots;

   public HotbarService(LobbySystem plugin) {
      this.plugin = plugin;
      this.messageService = plugin.getMessageService();
      this.hotbarKey = new NamespacedKey(plugin, "hotbar_item");
      this.reload();
   }

   // Cached item templates
   public void reload() {
      this.hiderTemplates = new HiderTemplates(
         this.messageService.component("hotbar.hider.name.shown", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(All)"),
         this.messageService.component("hotbar.hider.name.ops", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(Staff Only)"),
         this.messageService.component("hotbar.hider.name.hidden", "<gradient:#FF8FA3:#FFB3C1>Players <#E8E8E8>(Hidden)"),
         this.messageService.componentList("hotbar.hider.lore.shown", List.of("<#6E6E6E>- <#E8E8E8>Hide regular players")),
         this.messageService.componentList("hotbar.hider.lore.ops", List.of("<#6E6E6E>- <#E8E8E8>Show staff only")),
         this.messageService.componentList("hotbar.hider.lore.hidden", List.of("<#6E6E6E>- <#E8E8E8>Show all players again"))
      );

      List<Integer> configuredSlots = new ArrayList<>();
      for (HotbarItemConfig item : this.plugin.getConfigService().get().hotbar().items()) {
         if (item.enabled()) {
            configuredSlots.add(item.slot());
         }
      }

      this.hotbarSlots = List.copyOf(configuredSlots);
   }

   // Player inventory operations
   public void giveHotbarItems(Player player) {
      this.removeHotbarItems(player);
      for (HotbarItemConfig item : this.plugin.getConfigService().get().hotbar().items()) {
         if (item.enabled()) {
            this.setHotbarItem(player, item.slot(), this.buildItem(item, player));
         }
      }
   }

   public void updatePlayerHiderItem(Player player) {
      this.findItem(HotbarAction.PLAYER_HIDER).ifPresent((item) -> this.setHotbarItem(player, item.slot(), this.buildItem(item, player)));
   }

   public void removeHotbarItems(Player player) {
      for (int slot = 0; slot < player.getInventory().getSize(); ++slot) {
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

   /** Returns the id (config key) of the hotbar item, or null if this isn't one. */
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

   public Optional<HotbarItemConfig> findById(String id) {
      if (id == null) {
         return Optional.empty();
      }

      return this.plugin.getConfigService().get().hotbar().items().stream().filter((item) -> item.id().equals(id)).findFirst();
   }

   public boolean isHotbarSlot(int slot) {
      return this.hotbarSlots.contains(slot);
   }

   public List<Integer> getHotbarSlots() {
      return this.hotbarSlots;
   }

   // Item building
   private Optional<HotbarItemConfig> findItem(HotbarAction action) {
      return this.plugin.getConfigService().get().hotbar().items().stream().filter((item) -> item.enabled() && item.action() == action).findFirst();
   }

   private ItemStack buildItem(HotbarItemConfig item, Player player) {
      if (item.action() == HotbarAction.PLAYER_HIDER) {
         PlayerVisibilityState state = this.plugin.getPlayerStateService().getPlayerVisibilityState(player.getUniqueId());
         HiderTemplates templates = this.hiderTemplates;
         Component name = switch (state) {
            case STAFF_ONLY -> templates.nameOps();
            case HIDDEN -> templates.nameHidden();
            case ALL -> templates.nameShown();
         };
         List<Component> lore = switch (state) {
            case STAFF_ONLY -> templates.loreOps();
            case HIDDEN -> templates.loreHidden();
            case ALL -> templates.loreShown();
         };
         return this.tagItem(ItemFactory.createNamedItem(item.material(), name, lore), item.id());
      }

      return this.tagItem(ItemFactory.createNamedItem(item.material(), item.name(), item.lore()), item.id());
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

   private ItemStack tagItem(ItemStack item, String id) {
      item.editMeta((meta) -> meta.getPersistentDataContainer().set(this.hotbarKey, PersistentDataType.STRING, id));
      return item;
   }

   private record HiderTemplates(
      Component nameShown,
      Component nameOps,
      Component nameHidden,
      List<Component> loreShown,
      List<Component> loreOps,
      List<Component> loreHidden
   ) {
   }
}
