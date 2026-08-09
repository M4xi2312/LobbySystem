package de.maximanu.lobbySystem;

import de.maximanu.lobbySystem.command.LobbyCommands;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.listener.CosmeticsListener;
import de.maximanu.lobbySystem.listener.DoubleJumpListener;
import de.maximanu.lobbySystem.listener.HotbarListener;
import de.maximanu.lobbySystem.listener.MenuListener;
import de.maximanu.lobbySystem.listener.PlayerLifecycleListener;
import de.maximanu.lobbySystem.listener.ProtectionListener;
import de.maximanu.lobbySystem.menu.CosmeticsMenu;
import de.maximanu.lobbySystem.menu.ServerSelectorMenu;
import de.maximanu.lobbySystem.service.BuildModeService;
import de.maximanu.lobbySystem.service.CosmeticService;
import de.maximanu.lobbySystem.service.DoubleJumpService;
import de.maximanu.lobbySystem.service.HotbarService;
import de.maximanu.lobbySystem.service.LobbyEnvironmentService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.service.PlayerStateService;
import de.maximanu.lobbySystem.service.SpawnService;
import de.maximanu.lobbySystem.service.VisibilityService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbySystem extends JavaPlugin {
   // Copied into items/hotbar/ and items/cosmetics/ the first time the plugin runs (i.e. only if
   // those folders don't exist yet) - deleting a file removes that item permanently, reloads never
   // recreate it. See ConfigService, which scans these folders instead of reading items from config.yml.
   private static final List<String> DEFAULT_HOTBAR_ITEMS = List.of("info", "server-selector", "player-hider", "rules", "cosmetics", "hub", "magic-wand");
   private static final List<String> DEFAULT_COSMETICS = List.of("party-hat", "diamond-crown", "flame-trail", "heart-trail", "firework-launcher", "grappling-hook", "pearl-bow");

   private MessageService messageService;
   private ConfigService configService;
   private HotbarService hotbarService;
   private ServerSelectorMenu serverSelectorMenu;
   private PlayerStateService playerStateService;
   private SpawnService spawnService;
   private VisibilityService visibilityService;
   private DoubleJumpService doubleJumpService;
   private BuildModeService buildModeService;
   private LobbyPlayerService lobbyPlayerService;
   private LobbyWorldService lobbyWorldService;
   private LobbyEnvironmentService lobbyEnvironmentService;
   private CosmeticService cosmeticService;
   private CosmeticsMenu cosmeticsMenu;

   @Override
   public void onEnable() {
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      this.saveDefaultConfig();
      this.reloadConfig();
      this.extractDefaultItemsIfMissing();

      // Core service wiring
      this.messageService = new MessageService(this);
      this.configService = new ConfigService(this, this.messageService);
      this.playerStateService = new PlayerStateService();
      this.spawnService = new SpawnService(this);
      this.lobbyWorldService = new LobbyWorldService(this);
      this.lobbyEnvironmentService = new LobbyEnvironmentService(this);
      this.visibilityService = new VisibilityService(this);
      this.doubleJumpService = new DoubleJumpService(this);
      this.buildModeService = new BuildModeService(this);
      this.hotbarService = new HotbarService(this);
      this.serverSelectorMenu = new ServerSelectorMenu(this, this.configService);
      this.cosmeticService = new CosmeticService(this);
      this.lobbyPlayerService = new LobbyPlayerService(this);
      this.cosmeticsMenu = new CosmeticsMenu(this);
      this.spawnService.reload();
      this.lobbyEnvironmentService.start();

      this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (event) -> LobbyCommands.register(this, event.registrar()));

      Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);
      Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
      Bukkit.getPluginManager().registerEvents(new HotbarListener(this), this);
      Bukkit.getPluginManager().registerEvents(new DoubleJumpListener(this), this);
      Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
      Bukkit.getPluginManager().registerEvents(new CosmeticsListener(this), this);

      this.reloadStartupStateDelayed();
      this.getLogger().info("LobbySystem enabled");
   }

   @Override
   public void onDisable() {
      if (this.lobbyEnvironmentService != null) {
         this.lobbyEnvironmentService.stop();
      }

      this.getLogger().info("LobbySystem disabled");
   }

   public void reloadPluginConfig() {
      this.saveDefaultConfig();
      this.reloadConfig();
      this.messageService.reload();
      this.configService.reload();
      this.spawnService.reload();
      this.hotbarService.reload();
      this.serverSelectorMenu.reloadMessages();
      this.cosmeticsMenu.reloadMessages();
      this.lobbyEnvironmentService.reload();
   }

   private void extractDefaultItemsIfMissing() {
      this.extractDefaultItemsIfMissing("items/hotbar", DEFAULT_HOTBAR_ITEMS);
      this.extractDefaultItemsIfMissing("items/cosmetics", DEFAULT_COSMETICS);
   }

   private void extractDefaultItemsIfMissing(String relativeFolder, List<String> ids) {
      if (new File(this.getDataFolder(), relativeFolder).exists()) {
         return;
      }

      for (String id : ids) {
         this.saveResource(relativeFolder + "/" + id + ".yml", false);
      }
   }

   private void reloadStartupStateDelayed() {
      this.getServer().getGlobalRegionScheduler().runDelayed(this, (task) -> {
         this.spawnService.reload();
         this.lobbyEnvironmentService.reload();
      }, 1L);
   }

   public MessageService getMessageService() {
      return this.messageService;
   }

   public ConfigService getConfigService() {
      return this.configService;
   }

   public HotbarService getHotbarService() {
      return this.hotbarService;
   }

   public ServerSelectorMenu getServerSelectorMenu() {
      return this.serverSelectorMenu;
   }

   public PlayerStateService getPlayerStateService() {
      return this.playerStateService;
   }

   public SpawnService getSpawnService() {
      return this.spawnService;
   }

   public VisibilityService getVisibilityService() {
      return this.visibilityService;
   }

   public DoubleJumpService getDoubleJumpService() {
      return this.doubleJumpService;
   }

   public BuildModeService getBuildModeService() {
      return this.buildModeService;
   }

   public LobbyPlayerService getLobbyPlayerService() {
      return this.lobbyPlayerService;
   }

   public LobbyWorldService getLobbyWorldService() {
      return this.lobbyWorldService;
   }

   public LobbyEnvironmentService getLobbyEnvironmentService() {
      return this.lobbyEnvironmentService;
   }

   public CosmeticService getCosmeticService() {
      return this.cosmeticService;
   }

   public CosmeticsMenu getCosmeticsMenu() {
      return this.cosmeticsMenu;
   }
}
