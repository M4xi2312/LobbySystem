package de.maximanu.lobbySystem.config;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * One configurable hotbar item. {@code name}/{@code lore} are the static display used for most
 * actions; {@link HotbarAction#PLAYER_HIDER} ignores them and renders its own state-dependent text.
 * {@code value} is action-specific: the command string for {@link HotbarAction#RUN_COMMAND}, or a
 * free-form tag for {@link HotbarAction#RUN_SCRIPT} that a script can read off {@code LobbyItemUseEvent}.
 * {@code message} is only used by {@link HotbarAction#SHOW_TEXT}.
 */
public record HotbarItemConfig(
   String id,
   boolean enabled,
   int slot,
   Material material,
   Component name,
   List<Component> lore,
   HotbarAction action,
   String value,
   List<ChatLineConfig> message
) {
}
