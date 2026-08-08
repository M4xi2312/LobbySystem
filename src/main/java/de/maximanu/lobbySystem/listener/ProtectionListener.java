package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;

/** Cancels world/inventory interactions inside the lobby world according to {@code protection.*} config. */
public class ProtectionListener implements Listener {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final LobbyPlayerService lobbyPlayerService;
   private final LobbyWorldService lobbyWorldService;

   public ProtectionListener(LobbySystem plugin) {
      this.plugin = plugin;
      this.configService = plugin.getConfigService();
      this.lobbyPlayerService = plugin.getLobbyPlayerService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onDamage(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof Player player) || !this.lobbyPlayerService.shouldProtect(player)) {
         return;
      }

      if (event.getCause() == DamageCause.VOID && this.configService.get().spawn().enabled() && this.configService.get().spawn().teleportOnVoid()) {
         if (this.lobbyPlayerService.teleportToSpawnIfSet(player, false, () -> this.lobbyPlayerService.refreshPlayer(player))) {
            event.setCancelled(true);
            return;
         }
      }

      if (this.configService.get().protection().damage()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onHunger(FoodLevelChangeEvent event) {
      HumanEntity entity = event.getEntity();
      if (entity instanceof Player player && this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().hunger()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().blockBreak() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBlockPlace(BlockPlaceEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().blockPlace() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityChangeBlock(EntityChangeBlockEvent event) {
      if (event.getBlock().getType() != Material.FARMLAND || !this.configService.get().protection().enabled() || !this.configService.get().protection().farmlandTrample()) {
         return;
      }

      if (event.getEntity() instanceof Player player) {
         if (this.lobbyWorldService.isLobbyWorld(player) && !this.lobbyPlayerService.isBuildMode(player)) {
            event.setCancelled(true);
         }

         return;
      }

      if (this.lobbyWorldService.isLobbyWorld(event.getBlock().getWorld())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onWeatherChange(WeatherChangeEvent event) {
      if (event.toWeatherState() && this.shouldProtectWorld(event.getWorld()) && this.configService.get().protection().weatherLock()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onThunderChange(ThunderChangeEvent event) {
      if (event.toThunderState() && this.shouldProtectWorld(event.getWorld()) && this.configService.get().protection().weatherLock()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onTimeSkip(TimeSkipEvent event) {
      if (this.plugin.getLobbyEnvironmentService().isApplyingTimeLock()) {
         return;
      }

      if (this.shouldProtectWorld(event.getWorld()) && this.configService.get().protection().timeLock()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (this.shouldProtectWorld(event.getLocation().getWorld()) && this.configService.get().protection().mobSpawning()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.HAND) {
         return;
      }

      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().interact() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityInteract(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof ItemFrame && this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().itemFrameRotate() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().entityInteract() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().armorStandEdit() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBucketEmpty(PlayerBucketEmptyEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().buckets() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBucketFill(PlayerBucketFillEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().buckets() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPlayerPortal(PlayerPortalEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.get().protection().portalUse() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityPortal(EntityPortalEvent event) {
      if (!(event.getEntity() instanceof Player) && this.shouldProtectWorld(event.getFrom().getWorld()) && this.configService.get().protection().portalUse()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onHangingBreak(HangingBreakEvent event) {
      if (!this.shouldProtectWorld(event.getEntity().getWorld()) || !this.configService.get().protection().hangingBreak()) {
         return;
      }

      if (event instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() instanceof Player player && this.lobbyPlayerService.isBuildMode(player)) {
         return;
      }

      event.setCancelled(true);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || MenuListener.isMenu(event.getView())) {
         return;
      }

      if (this.lobbyPlayerService.shouldBlockCreativePick(player) && this.isCreativeCloneAttempt(event)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().inventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (!(event.getWhoClicked() instanceof Player player) || MenuListener.isMenu(event.getView())) {
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().inventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().inventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.get().protection().itemDrop() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player player)) {
         return;
      }

      if (this.lobbyWorldService.isLobbyWorld(player) && this.configService.get().protection().itemPickup() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickBlock(PlayerPickBlockEvent event) {
      if (this.lobbyPlayerService.shouldBlockCreativePick(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickEntity(PlayerPickEntityEvent event) {
      if (this.lobbyPlayerService.shouldBlockCreativePick(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   private boolean isCreativeCloneAttempt(InventoryClickEvent event) {
      return event.getAction() == InventoryAction.CLONE_STACK || event.getClick() == ClickType.CREATIVE;
   }

   private boolean shouldProtectWorld(World world) {
      return this.configService.get().protection().enabled() && this.lobbyWorldService.isLobbyWorld(world);
   }
}
