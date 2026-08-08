package de.maximanu.lobbySystem.menu;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.CosmeticConfig;
import de.maximanu.lobbySystem.config.CosmeticKind;
import de.maximanu.lobbySystem.config.CosmeticsConfig;
import de.maximanu.lobbySystem.service.CosmeticService;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.util.ItemFactory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Cosmetics browser built on {@link AbstractMenu}: a category tab row (hats/particles/gadgets) plus a grid. */
public class CosmeticsMenu {
   private final LobbySystem plugin;
   private final MessageService messageService;

   private volatile MenuTemplates templates;

   public CosmeticsMenu(LobbySystem plugin) {
      this.plugin = plugin;
      this.messageService = plugin.getMessageService();
      this.reloadMessages();
   }

   public void reloadMessages() {
      Component title = ItemFactory.nonItalic(this.messageService.component("menu.cosmetics.title", "<gradient:#B39DFF:#7EE8FA>Cosmetics"));
      Component hatsTab = this.messageService.component("menu.cosmetics.tab.hats", "<gradient:#FFE082:#FFB347>Hats");
      Component particlesTab = this.messageService.component("menu.cosmetics.tab.particles", "<gradient:#7EE8FA:#5AA9FF>Particles");
      Component gadgetsTab = this.messageService.component("menu.cosmetics.tab.gadgets", "<gradient:#FF8FA3:#FFB3C1>Gadgets");
      Component fillerName = this.messageService.component("menu.cosmetics.filler-name", " ");
      ItemStack fillerItem = ItemFactory.createNamedItem(Material.GRAY_STAINED_GLASS_PANE, fillerName, List.of());
      this.templates = new MenuTemplates(title, hatsTab, particlesTab, gadgetsTab, fillerItem);
   }

   public void open(Player player) {
      this.open(player, CosmeticKind.HAT);
   }

   public void open(Player player, CosmeticKind category) {
      CosmeticsConfig cosmeticsConfig = this.plugin.getConfigService().get().cosmetics();
      if (!cosmeticsConfig.enabled()) {
         this.plugin.getLobbyPlayerService().sendFeatureDisabled(player, "cosmetics");
         return;
      }

      MenuTemplates templates = this.templates;
      int size = cosmeticsConfig.menuSize();
      GuiMenu menu = new GuiMenu(size, templates.title());

      List<CosmeticConfig> items = cosmeticsConfig.itemsOf(category).stream().filter(CosmeticConfig::enabled).toList();
      int gridSlots = Math.max(0, size - 9);
      for (int i = 0; i < items.size() && i < gridSlots; i++) {
         this.setCosmeticItem(menu, i, items.get(i), player, category);
      }

      int rowStart = size - 9;
      if (rowStart >= 0) {
         menu.set(rowStart + 2, this.buildTabItem(Material.LEATHER_HELMET, templates.hatsTab(), category == CosmeticKind.HAT), (event) -> this.open(player, CosmeticKind.HAT));
         menu.set(rowStart + 4, this.buildTabItem(Material.FIREWORK_STAR, templates.particlesTab(), category == CosmeticKind.PARTICLE), (event) -> this.open(player, CosmeticKind.PARTICLE));
         menu.set(rowStart + 6, this.buildTabItem(Material.FIREWORK_ROCKET, templates.gadgetsTab(), category == CosmeticKind.GADGET), (event) -> this.open(player, CosmeticKind.GADGET));
      }

      menu.fillEmpty(templates.fillerItem());
      menu.open(player);
   }

   private void setCosmeticItem(GuiMenu menu, int slot, CosmeticConfig cosmetic, Player player, CosmeticKind category) {
      CosmeticService cosmeticService = this.plugin.getCosmeticService();

      if (!cosmetic.isUnlockedFor(player)) {
         Component name = this.messageService.component("menu.cosmetics.locked.name", "<gray>Locked");
         List<Component> lore = this.messageService.componentList("menu.cosmetics.locked.lore", List.of("<#6E6E6E>- <#E8E8E8>You don't have permission for this."));
         menu.set(slot, ItemFactory.createNamedItem(Material.BARRIER, name, lore));
         return;
      }

      boolean equipped = switch (category) {
         case HAT -> cosmetic.id().equals(cosmeticService.getEquippedHat(player));
         case PARTICLE -> cosmetic.id().equals(cosmeticService.getEquippedParticle(player));
         case GADGET -> false;
      };

      List<Component> lore = new ArrayList<>(cosmetic.lore());
      if (category == CosmeticKind.GADGET) {
         lore.add(this.messageService.component("menu.cosmetics.click-to-receive", "<#6E6E6E>- <#E8E8E8>Click to receive"));
      } else if (equipped) {
         lore.add(this.messageService.component("menu.cosmetics.equipped", "<#6E6E6E>- <green>Equipped - click to unequip"));
      } else {
         lore.add(this.messageService.component("menu.cosmetics.click-to-equip", "<#6E6E6E>- <#E8E8E8>Click to equip"));
      }

      ItemStack item = ItemFactory.createNamedItem(cosmetic.material(), cosmetic.name(), lore);
      if (equipped) {
         item.editMeta((meta) -> meta.setEnchantmentGlintOverride(true));
      }

      menu.set(slot, item, (event) -> {
         switch (category) {
            case HAT -> {
               cosmeticService.toggleHat(player, cosmetic.id());
               this.open(player, CosmeticKind.HAT);
            }
            case PARTICLE -> {
               cosmeticService.toggleParticle(player, cosmetic.id());
               this.open(player, CosmeticKind.PARTICLE);
            }
            case GADGET -> {
               cosmeticService.giveGadget(player, cosmetic.id());
               player.closeInventory();
            }
         }
      });
   }

   private ItemStack buildTabItem(Material material, Component name, boolean active) {
      ItemStack item = ItemFactory.createNamedItem(material, name, List.of());
      if (active) {
         item.editMeta((meta) -> meta.setEnchantmentGlintOverride(true));
      }

      return item;
   }

   private record MenuTemplates(Component title, Component hatsTab, Component particlesTab, Component gadgetsTab, ItemStack fillerItem) {
   }

   private static final class GuiMenu extends AbstractMenu {
      private GuiMenu(int size, Component title) {
         super(size, title);
      }
   }
}
