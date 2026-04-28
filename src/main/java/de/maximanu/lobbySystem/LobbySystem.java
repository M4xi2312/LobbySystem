package de.maximanu.lobbySystem;

import de.maximanu.lobbySystem.commands.BuildCommand;
import de.maximanu.lobbySystem.commands.LobbySystemCommand;
import de.maximanu.lobbySystem.commands.SetSpawnCommand;
import de.maximanu.lobbySystem.commands.SpawnCommand;
import de.maximanu.lobbySystem.config.ConfigService;
import de.maximanu.lobbySystem.listener.PlayerListener;
import de.maximanu.lobbySystem.menu.ServerSelectorMenu;
import de.maximanu.lobbySystem.service.BuildModeService;
import de.maximanu.lobbySystem.service.DoubleJumpService;
import de.maximanu.lobbySystem.service.HotbarService;
import de.maximanu.lobbySystem.service.LobbyPlayerService;
import de.maximanu.lobbySystem.service.LobbyWorldService;
import de.maximanu.lobbySystem.service.MessageService;
import de.maximanu.lobbySystem.service.PlayerStateService;
import de.maximanu.lobbySystem.service.SpawnService;
import de.maximanu.lobbySystem.service.VisibilityService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbySystem extends JavaPlugin {
   private PlayerListener playerListener;
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

   public void onEnable() {
      // Core service wiring
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      this.saveDefaultConfig();
      this.messageService = new MessageService(this);
      this.configService = new ConfigService(this, this.messageService);
      this.playerStateService = new PlayerStateService();
      this.spawnService = new SpawnService(this);
      this.lobbyWorldService = new LobbyWorldService(this);
      this.visibilityService = new VisibilityService(this);
      this.doubleJumpService = new DoubleJumpService(this);
      this.buildModeService = new BuildModeService(this);
      this.hotbarService = new HotbarService(this);
      this.serverSelectorMenu = new ServerSelectorMenu(this, this.configService);
      this.lobbyPlayerService = new LobbyPlayerService(this);

      // Command and event registration
      this.registerCommand("spawn", new SpawnCommand(this));
      this.registerCommand("setspawn", new SetSpawnCommand(this));
      this.registerCommand("build", new BuildCommand(this));
      this.registerCommand("lobbysystem", new LobbySystemCommand(this));
      this.playerListener = new PlayerListener(this);
      Bukkit.getPluginManager().registerEvents(this.playerListener, this);
      this.getLogger().info("LobbySystem enabled");
   }

   public void onDisable() {
      this.saveConfig();
      this.getLogger().info("LobbySystem disabled");
   }

   public PlayerListener getPlayerListener() {
      return this.playerListener;
   }

   public void reloadPluginConfig() {
      this.reloadConfig();
      this.saveDefaultConfig();
      this.messageService.reload();
      this.configService.reload();
      this.spawnService.reload();
      this.hotbarService.reload();
      this.serverSelectorMenu.reloadMessages();
   }

   private void registerCommand(String name, CommandExecutor executor) {
      if (this.getCommand(name) == null) {
         this.getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
      } else {
         this.getCommand(name).setExecutor(executor);
      }
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
}
