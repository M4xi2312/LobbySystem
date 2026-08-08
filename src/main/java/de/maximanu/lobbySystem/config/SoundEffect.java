package de.maximanu.lobbySystem.config;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public record SoundEffect(Sound sound, float volume, float pitch) {

   public void play(Player player, boolean soundsEnabled) {
      if (soundsEnabled && player != null && this.sound != null) {
         player.playSound(player.getLocation(), this.sound, this.volume, this.pitch);
      }
   }
}
