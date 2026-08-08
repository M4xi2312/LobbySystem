package de.maximanu.lobbySystem.config;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * One configurable cosmetic. {@code particle}/{@code particleIntervalTicks} only apply to
 * {@link CosmeticKind#PARTICLE}; {@code gadgetEffect}/{@code gadgetCooldownTicks}/{@code gadgetSound}
 * only apply to {@link CosmeticKind#GADGET}. An empty {@code permission} means the cosmetic is
 * unlocked for everyone.
 */
public record CosmeticConfig(
   String id,
   CosmeticKind kind,
   boolean enabled,
   Material material,
   Component name,
   List<Component> lore,
   String permission,
   Particle particle,
   int particleIntervalTicks,
   GadgetEffect gadgetEffect,
   int gadgetCooldownTicks,
   SoundEffect gadgetSound
) {

   public boolean isUnlockedFor(Player player) {
      return this.permission.isBlank() || player.hasPermission(this.permission);
   }
}
