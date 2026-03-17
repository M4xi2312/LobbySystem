package de.maximanu.lobbySystem.listener;

import de.maximanu.lobbySystem.LobbySystem;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.menu.ServerSelectorMenu;
import de.maximanu.lobbySystem.service.HotbarService;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.service.PlayerStateService;
import de.maximanu.lobbySystem.service.SpawnService;
import de.maximanu.lobbySystem.service.VisibilityService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final LobbySystem plugin;
    private final ConfigService configService;
    private final HotbarService hotbarService;
    private final ServerSelectorMenu serverSelectorMenu;
    private final MessageService messageService;
    private final PlayerStateService playerStateService;
    private final SpawnService spawnService;
    private final VisibilityService visibilityService;

    public PlayerListener(LobbySystem plugin) {
        this.plugin = plugin;
        this.configService = plugin.getConfigService();
        this.hotbarService = plugin.getHotbarService();
        this.serverSelectorMenu = plugin.getServerSelectorMenu();
        this.messageService = plugin.getMessageService();
        this.playerStateService = plugin.getPlayerStateService();
        this.spawnService = plugin.getSpawnService();
        this.visibilityService = plugin.getVisibilityService();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (configService.isTeleportOnJoin()) {
            teleportToSpawnIfSet(p);
        }
        updateHotbarState(p);
        visibilityService.applyVisibility(p, playerStateService.getPlayerHiderState(p.getUniqueId()));
        updateVisibilityForOthers(p);
        updateDoubleJumpState(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Location spawn = spawnService.getSpawnLocation();
        if (configService.isTeleportOnRespawn() && spawn != null) {
            e.setRespawnLocation(spawn);
        }
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateHotbarState(p);
            visibilityService.applyVisibility(p, playerStateService.getPlayerHiderState(p.getUniqueId()));
            updateDoubleJumpState(p);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!shouldProtect(p)) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
            if (configService.isTeleportOnVoid()) {
                if (teleportToSpawnIfSet(p)) {
                    e.setCancelled(true);
                    return;
                }
                return;
            }
            if (configService.isProtectDamage()) {
                e.setCancelled(true);
            }
            return;
        }
        if (configService.isProtectDamage()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!shouldProtect(player)) return;
        if (configService.isProtectHunger()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        if (configService.isProtectBlockBreak() && !isBuildMode(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        if (configService.isProtectBlockPlace() && !isBuildMode(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Action action = e.getAction();

        boolean inLobbyWorld = isLobbyWorld(p);
        boolean protect = configService.isProtectAllWorlds() || inLobbyWorld;
        boolean hotbarActive = configService.isHotbarEnabled()
                && (configService.isHotbarAllWorlds() || inLobbyWorld);

        if (protect && configService.isProtectInteract() && !isBuildMode(p)) {
            e.setCancelled(true);
        }

        if (!hotbarActive) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        String type = hotbarService.getHotbarType(hand);
        if (type == null) return;

        if (type.equals("info")) {
            sendLinks(p);
            playSound(p, configService.getSoundInfo(), configService.getSoundInfoVolume(), configService.getSoundInfoPitch());
        } else if (type.equals("selector")) {
            serverSelectorMenu.open(p);
            playSound(p, configService.getSoundSelectorOpen(), configService.getSoundSelectorOpenVolume(), configService.getSoundSelectorOpenPitch());
        } else if (type.equals("hider")) {
            togglePlayerHider(p);
            playSound(p, configService.getSoundHiderToggle(), configService.getSoundHiderToggleVolume(), configService.getSoundHiderTogglePitch());
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        if (configService.isProtectEntityInteract() && !isBuildMode(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (serverSelectorMenu.isSelectorView(e.getView())) {
            e.setCancelled(true);
            serverSelectorMenu.handleClick(p, e.getCurrentItem(), e.getView());
            return;
        }

        if (!shouldProtect(p)) return;
        if (configService.isProtectInventory() && !isBuildMode(p)) {
            e.setCancelled(true);
            return;
        }
        if (shouldHotbarLock(p) && isLockedHotbarInteraction(p, e)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            e.setCancelled(true);
            return;
        }
        if (serverSelectorMenu.isSelectorView(e.getView())) {
            e.setCancelled(true);
            return;
        }
        if (!shouldProtect(p)) return;
        if (configService.isProtectInventory() && !isBuildMode(p)) {
            e.setCancelled(true);
            return;
        }
        if (shouldHotbarLock(p)) {
            for (int rawSlot : e.getRawSlots()) {
                if (isHotbarRawSlot(rawSlot, e.getView())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!shouldProtect(e.getPlayer())) return;
        if (configService.isProtectItemDrop() && !isBuildMode(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (shouldHotbarLock(e.getPlayer()) && hotbarService.isHotbarItem(dropped)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!shouldDoubleJump(p)) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        e.setCancelled(true);
        p.setAllowFlight(false);
        Vector velocity = p.getLocation().getDirection().multiply(configService.getDoubleJumpForward());
        velocity.setY(configService.getDoubleJumpUp());
        p.setVelocity(velocity);
        playSound(p, configService.getSoundDoubleJump(), configService.getSoundDoubleJumpVolume(), configService.getSoundDoubleJumpPitch());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getAllowFlight()) return;
        if (!p.isOnGround()) return;
        if (!shouldDoubleJump(p)) return;
        p.setAllowFlight(true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        updateHotbarState(p);
        updateDoubleJumpState(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playerStateService.clearPlayer(e.getPlayer().getUniqueId());
    }

    private void togglePlayerHider(Player p) {
        UUID id = p.getUniqueId();
        int state = (playerStateService.getPlayerHiderState(id) + 1) % 3;
        playerStateService.setPlayerHiderState(id, state);
        visibilityService.applyVisibility(p, state);
        hotbarService.updatePlayerHiderItem(p);
        String msg = switch (state) {
            case 0 -> messageService.get("info.visibility.all", "&bPlayer visibility: &7All players shown");
            case 1 -> messageService.get("info.visibility.ops", "&bPlayer visibility: &7Only ops shown");
            default -> messageService.get("info.visibility.hidden", "&bPlayer visibility: &7All players hidden");
        };
        p.sendMessage(msg);
    }

    private void sendLinks(Player p) {
        p.sendMessage(messageService.format(
                "links.website",
                "&aWebsite: &7{link}",
                java.util.Map.of("link", configService.getLink("website", "https://example.com"))
        ));
        p.sendMessage(messageService.format(
                "links.discord",
                "&9Discord: &7{link}",
                java.util.Map.of("link", configService.getLink("discord", "https://discord.gg/example"))
        ));
        p.sendMessage(messageService.format(
                "links.store",
                "&6Store: &7{link}",
                java.util.Map.of("link", configService.getLink("store", "https://store.example.com"))
        ));
    }

    private boolean teleportToSpawnIfSet(Player p) {
        Location spawn = spawnService.getSpawnLocation();
        if (spawn != null) {
            p.teleport(spawn);
            playSound(p, configService.getSoundTeleport(), configService.getSoundTeleportVolume(), configService.getSoundTeleportPitch());
            return true;
        }
        if (p.isOp()) {
            p.sendMessage(messageService.get("errors.spawn-not-set-op", "&cSpawn is not set. Use /setspawn."));
        }
        return false;
    }

    private void updateVisibilityForOthers(Player joined) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(joined)) continue;
            int state = playerStateService.getPlayerHiderState(other.getUniqueId());
            visibilityService.applyVisibilityToTarget(other, joined, state);
        }
    }

    public void refreshAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refreshPlayer(p);
        }
    }

    public void refreshPlayer(Player p) {
        updateHotbarState(p);
        visibilityService.applyVisibility(p, playerStateService.getPlayerHiderState(p.getUniqueId()));
        updateVisibilityForOthers(p);
        updateDoubleJumpState(p);
    }

    private boolean isLobbyWorld(Player p) {
        String worldName = configService.getLobbyWorldName();
        if (worldName.isEmpty()) return true;
        return p.getWorld().getName().equalsIgnoreCase(worldName);
    }

    private boolean shouldProtect(Player p) {
        return configService.isProtectAllWorlds() || isLobbyWorld(p);
    }

    private boolean shouldGiveHotbar(Player p) {
        return configService.isHotbarEnabled()
                && (configService.isHotbarAllWorlds() || isLobbyWorld(p))
                && !isBuildMode(p);
    }

    private boolean shouldDoubleJump(Player p) {
        return configService.isDoubleJumpEnabled()
                && (configService.isDoubleJumpAllWorlds() || isLobbyWorld(p));
    }

    private void updateHotbarState(Player p) {
        if (shouldGiveHotbar(p)) {
            hotbarService.giveHotbarItems(p);
        } else {
            hotbarService.removeHotbarItems(p);
        }
    }

    private void updateDoubleJumpState(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        boolean allow = shouldDoubleJump(p);
        if (p.getAllowFlight() != allow) {
            p.setAllowFlight(allow);
        }
    }

    private void playSound(Player p, Sound sound, float volume, float pitch) {
        if (sound == null) return;
        p.playSound(p.getLocation(), sound, volume, pitch);
    }

    private boolean shouldHotbarLock(Player p) {
        return configService.isHotbarEnabled()
                && configService.isHotbarLockEnabled()
                && (configService.isHotbarAllWorlds() || isLobbyWorld(p))
                && !isBuildMode(p);
    }

    private boolean isBuildMode(Player p) {
        return playerStateService.getBuildMode(p.getUniqueId());
    }

    private boolean isLockedHotbarInteraction(Player p, InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(p.getInventory())) {
            int slot = e.getSlot();
            if (!hotbarService.isHotbarSlot(slot)) return false;
        }

        ItemStack current = e.getCurrentItem();
        if (hotbarService.isHotbarItem(current)) return true;

        ItemStack cursor = e.getCursor();
        if (hotbarService.isHotbarItem(cursor)) return true;

        int hotbarButton = e.getHotbarButton();
        if (hotbarButton >= 0) {
            if (!hotbarService.isHotbarSlot(hotbarButton)) return false;
            ItemStack hotbarItem = p.getInventory().getItem(hotbarButton);
            return hotbarService.isHotbarItem(hotbarItem);
        }
        return false;
    }

    private boolean isHotbarRawSlot(int rawSlot, org.bukkit.inventory.InventoryView view) {
        int topSize = view.getTopInventory().getSize();
        int hotbarBase = topSize + 27;
        for (int slot : hotbarService.getHotbarSlots()) {
            if (rawSlot == hotbarBase + slot) return true;
        }
        return false;
    }
}
