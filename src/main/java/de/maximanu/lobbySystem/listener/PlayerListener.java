package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import java.util.Iterator;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {
   private final LobbySystem plugin;
   private final ConfigService configService;
   private final LobbyPlayerService lobbyPlayerService;
   private final LobbyWorldService lobbyWorldService;

   public PlayerListener(LobbySystem plugin) {
      this.plugin = plugin;
      this.configService = plugin.getConfigService();
      this.lobbyPlayerService = plugin.getLobbyPlayerService();
      this.lobbyWorldService = plugin.getLobbyWorldService();
   }

   // Player lifecycle
   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.lobbyPlayerService.handleJoin(event.getPlayer());
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      Location spawn = this.plugin.getSpawnService().getSpawnLocation();
      if (this.configService.isSpawnEnabled() && this.configService.isTeleportOnRespawn() && spawn != null) {
         event.setRespawnLocation(spawn);
      }

      this.lobbyPlayerService.handleRespawn(event.getPlayer());
   }

   @EventHandler
   public void onWorldChange(PlayerChangedWorldEvent event) {
      this.lobbyPlayerService.refreshPlayer(event.getPlayer());
   }

   @EventHandler
   public void onGameModeChange(PlayerGameModeChangeEvent event) {
      Player player = event.getPlayer();
      player.getScheduler().execute(this.plugin, () -> this.lobbyPlayerService.refreshPlayer(player), null, 1L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.plugin.getDoubleJumpService().clear(player, true);
      this.plugin.getBuildModeService().clearOnQuit(player);
      this.plugin.getPlayerStateService().clearVisibility(player.getUniqueId());
   }

   // Protection and interaction rules
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onDamage(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof Player player) || !this.lobbyPlayerService.shouldProtect(player)) {
         return;
      }

      if (event.getCause() == DamageCause.VOID && this.configService.isSpawnEnabled() && this.configService.isTeleportOnVoid()) {
         if (this.lobbyPlayerService.teleportToSpawnIfSet(player, false, () -> this.lobbyPlayerService.refreshPlayer(player))) {
            event.setCancelled(true);
            return;
         }
      }

      if (this.configService.isProtectDamage()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onHunger(FoodLevelChangeEvent event) {
      HumanEntity entity = event.getEntity();
      if (entity instanceof Player player && this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectHunger()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectBlockBreak() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBlockPlace(BlockPlaceEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectBlockPlace() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityChangeBlock(EntityChangeBlockEvent event) {
      if (event.getBlock().getType() != Material.FARMLAND || !this.configService.isProtectionEnabled() || !this.configService.isProtectFarmlandTrample()) {
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
      if (event.toWeatherState() && this.shouldProtectWorld(event.getWorld()) && this.configService.isProtectWeatherChange()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onThunderChange(ThunderChangeEvent event) {
      if (event.toThunderState() && this.shouldProtectWorld(event.getWorld()) && this.configService.isProtectWeatherChange()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onTimeSkip(TimeSkipEvent event) {
      if (this.shouldProtectWorld(event.getWorld()) && this.configService.isProtectTimeLock()) {
         event.setCancelled(true);
         event.getWorld().setTime(this.configService.getLockedTime());
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (this.shouldProtectWorld(event.getLocation().getWorld()) && this.configService.isProtectMobSpawning()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.HAND) {
         return;
      }

      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectInteract() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
      }

      if (!this.configService.isHotbarEnabled() || !this.lobbyWorldService.isLobbyWorld(player)) {
         return;
      }

      Action action = event.getAction();
      if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
         return;
      }

      ItemStack mainHand = player.getInventory().getItemInMainHand();
      String hotbarType = this.lobbyPlayerService.getHotbarService().getHotbarType(mainHand);
      if (hotbarType == null) {
         return;
      }

      switch(hotbarType) {
      case "info" -> {
         this.lobbyPlayerService.sendLinks(player);
         this.lobbyPlayerService.playSound(player, this.configService.getSoundInfo(), this.configService.getSoundInfoVolume(), this.configService.getSoundInfoPitch());
      }
      case "selector" -> {
         this.lobbyPlayerService.getServerSelectorMenu().open(player);
         this.lobbyPlayerService.playSound(player, this.configService.getSoundSelectorOpen(), this.configService.getSoundSelectorOpenVolume(), this.configService.getSoundSelectorOpenPitch());
      }
      case "hider" -> {
         this.lobbyPlayerService.togglePlayerHider(player);
         this.lobbyPlayerService.playSound(player, this.configService.getSoundHiderToggle(), this.configService.getSoundHiderToggleVolume(), this.configService.getSoundHiderTogglePitch());
      }
      default -> {
      }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityInteract(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof ItemFrame && this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectItemFrameRotate() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectEntityInteract() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectArmorStandEdit() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBucketEmpty(PlayerBucketEmptyEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectBuckets() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onBucketFill(PlayerBucketFillEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectBuckets() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPlayerPortal(PlayerPortalEvent event) {
      if (this.lobbyPlayerService.shouldProtect(event.getPlayer()) && this.configService.isProtectPortalUse() && !this.lobbyPlayerService.isBuildMode(event.getPlayer())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onEntityPortal(EntityPortalEvent event) {
      if (!(event.getEntity() instanceof Player) && this.shouldProtectWorld(event.getFrom().getWorld()) && this.configService.isProtectPortalUse()) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onHangingBreak(HangingBreakEvent event) {
      if (!this.shouldProtectWorld(event.getEntity().getWorld()) || !this.configService.isProtectHangingBreak()) {
         return;
      }

      if (event instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() instanceof Player player && this.lobbyPlayerService.isBuildMode(player)) {
         return;
      }

      event.setCancelled(true);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      if (this.lobbyPlayerService.getServerSelectorMenu().isSelectorView(event.getView())) {
         event.setCancelled(true);
         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            this.lobbyPlayerService.getServerSelectorMenu().handleClick(player, event.getCurrentItem(), event.getView());
         }

         return;
      }

      if (this.lobbyPlayerService.shouldBlockCreativePick(player) && this.isCreativeCloneAttempt(event)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectInventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldHotbarLock(player) && this.isLockedHotbarInteraction(player, event)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      if (this.lobbyPlayerService.getServerSelectorMenu().isSelectorView(event.getView())) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectInventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldHotbarLock(player)) {
         Iterator<Integer> iterator = event.getRawSlots().iterator();

         while(iterator.hasNext()) {
            int rawSlot = iterator.next();
            if (this.isHotbarRawSlot(rawSlot, event.getView())) {
               event.setCancelled(true);
               return;
            }
         }
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectInventory() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldHotbarLock(player) && (this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getMainHandItem()) || this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getOffHandItem()))) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.lobbyPlayerService.shouldProtect(player) && this.configService.isProtectItemDrop() && !this.lobbyPlayerService.isBuildMode(player)) {
         event.setCancelled(true);
         return;
      }

      if (this.lobbyPlayerService.shouldHotbarLock(player) && this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getItemDrop().getItemStack())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player player)) {
         return;
      }

      if (this.lobbyWorldService.isLobbyWorld(player) && this.configService.isProtectItemPickup() && !this.lobbyPlayerService.isBuildMode(player)) {
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

   // Double jump runtime
   @EventHandler(ignoreCancelled = true)
   public void onToggleFlight(PlayerToggleFlightEvent event) {
      Player player = event.getPlayer();
      if (!this.lobbyPlayerService.shouldDoubleJump(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
         return;
      }

      event.setCancelled(true);
      if (this.lobbyPlayerService.getDoubleJumpService().tryUse(player)) {
         player.setAllowFlight(false);
         Vector velocity = player.getLocation().getDirection().multiply(this.configService.getDoubleJumpForward());
         velocity.setY(this.configService.getDoubleJumpUp());
         player.setVelocity(velocity);
         this.lobbyPlayerService.playSound(player, this.configService.getSoundDoubleJump(), this.configService.getSoundDoubleJumpVolume(), this.configService.getSoundDoubleJumpPitch());
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
         return;
      }

      if (this.lobbyPlayerService.isBuildMode(player) || player.getAllowFlight() || !player.isOnGround() || !this.lobbyPlayerService.shouldDoubleJump(player)) {
         return;
      }

      if (!this.lobbyPlayerService.getDoubleJumpService().isOnCooldown(player)) {
         player.setAllowFlight(true);
      }
   }

   public void refreshAllPlayers() {
      this.lobbyPlayerService.refreshAllPlayers();
   }

   public void refreshPlayer(Player player) {
      this.lobbyPlayerService.refreshPlayer(player);
   }

   private boolean isLockedHotbarInteraction(Player player, InventoryClickEvent event) {
      if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory()) && this.lobbyPlayerService.getHotbarService().isHotbarSlot(event.getSlot())) {
         return true;
      }

      if (this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getCurrentItem()) || this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getCursor())) {
         return true;
      }

      int hotbarButton = event.getHotbarButton();
      if (hotbarButton >= 0 && this.lobbyPlayerService.getHotbarService().isHotbarSlot(hotbarButton)) {
         ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
         return this.lobbyPlayerService.getHotbarService().isHotbarItem(hotbarItem);
      }

      return event.getClick() == ClickType.SWAP_OFFHAND && (this.lobbyPlayerService.getHotbarService().isHotbarItem(player.getInventory().getItemInOffHand()) || this.lobbyPlayerService.getHotbarService().isHotbarItem(event.getCurrentItem()));
   }

   private boolean isCreativeCloneAttempt(InventoryClickEvent event) {
      return event.getAction() == InventoryAction.CLONE_STACK || event.getClick() == ClickType.CREATIVE;
   }

   private boolean isHotbarRawSlot(int rawSlot, InventoryView view) {
      int hotbarBase = view.getTopInventory().getSize() + 27;
      return this.lobbyPlayerService.getHotbarService().getHotbarSlots().stream().anyMatch((slot) -> rawSlot == hotbarBase + slot);
   }

   private boolean shouldProtectWorld(World world) {
      return this.configService.isProtectionEnabled() && this.lobbyWorldService.isLobbyWorld(world);
   }
}
