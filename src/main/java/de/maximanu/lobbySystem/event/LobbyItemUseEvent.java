package de.maximanu.lobbySystem.event;

import de.maximanu.lobbySystem.config.HotbarAction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever a player right-clicks a configured hotbar item, before its built-in action runs.
 * Cancel this to suppress the built-in action entirely (useful when a script wants to fully take over).
 *
 * <p>Other plugins - notably Skript - can listen to this like any other Bukkit event. Skript scripts
 * typically hook in with {@code on de.maximanu.lobbySystem.event.LobbyItemUseEvent:} (exact syntax
 * depends on your Skript version/addons for third-party events); the item id and the configured
 * {@code value} field (see {@code action: run-script} in config.yml) are exposed as getters so a
 * script can tell items apart.
 */
public class LobbyItemUseEvent extends PlayerEvent implements Cancellable {
   private static final HandlerList HANDLERS = new HandlerList();

   private final String itemId;
   private final HotbarAction action;
   private final String value;
   private boolean cancelled;

   public LobbyItemUseEvent(Player player, String itemId, HotbarAction action, String value) {
      super(player);
      this.itemId = itemId;
      this.action = action;
      this.value = value;
   }

   /** The id (config key) of the hotbar item that was used, e.g. {@code "info"}. */
   public String getItemId() {
      return this.itemId;
   }

   /** The action configured for this item. */
   public HotbarAction getItemAction() {
      return this.action;
   }

   /** The item's configured {@code value} field (the command for run-command, or a free-form tag for run-script). */
   public String getValue() {
      return this.value;
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean cancel) {
      this.cancelled = cancel;
   }

   @Override
   public @NotNull HandlerList getHandlers() {
      return HANDLERS;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS;
   }
}
