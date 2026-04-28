package de.maximanu.lobbySystem.service;

import de.maximanu.lobbySystem.LobbySystem;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MessageService {
   private final LobbySystem plugin;
   private FileConfiguration messages;
   private File messagesFile;
   private final MiniMessage miniMessage = MiniMessage.miniMessage();
   private final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacyAmpersand();
   private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();
   private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");
   private static final Pattern MINI_TAG_PATTERN = Pattern.compile("<[^>]+>");

   public MessageService(LobbySystem plugin) {
      this.plugin = plugin;
      this.reload();
   }

   public void reload() {
      if (this.messagesFile == null) {
         this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
      }

      if (!this.messagesFile.exists()) {
         this.plugin.saveResource("messages.yml", false);
      }

      this.messages = YamlConfiguration.loadConfiguration(this.messagesFile);
   }

   public String get(String key, String fallback) {
      return this.toLegacy(this.component(key, fallback));
   }

   public List<String> getList(String key, List<String> fallback) {
      LegacyComponentSerializer serializer = this.legacySection;
      Objects.requireNonNull(serializer);
      return this.componentList(key, fallback).stream().map(serializer::serialize).collect(Collectors.toList());
   }

   public String format(String key, String fallback, Map<String, String> vars) {
      return this.toLegacy(this.formatComponent(key, fallback, vars));
   }

   public List<String> formatList(String key, List<String> fallback, Map<String, String> vars) {
      LegacyComponentSerializer serializer = this.legacySection;
      Objects.requireNonNull(serializer);
      return this.formatComponentList(key, fallback, vars).stream().map(serializer::serialize).collect(Collectors.toList());
   }

   public Component component(String key, String fallback) {
      String raw = this.messages.getString(key, fallback);
      return this.deserialize(raw);
   }

   public List<Component> componentList(String key, List<String> fallback) {
      List<String> raw = this.messages.contains(key) ? this.messages.getStringList(key) : fallback;
      return raw.stream().map(this::deserialize).collect(Collectors.toList());
   }

   public Component formatComponent(String key, String fallback, Map<String, String> vars) {
      String raw = this.messages.getString(key, fallback);
      if (raw == null) {
         raw = fallback;
      }

      return this.deserialize(this.replaceVars(raw, vars));
   }

   public List<Component> formatComponentList(String key, List<String> fallback, Map<String, String> vars) {
      List<String> raw = this.messages.contains(key) ? this.messages.getStringList(key) : fallback;
      return raw.stream().map((line) -> {
         return this.deserialize(this.replaceVars(line, vars));
      }).collect(Collectors.toList());
   }

   public String toLegacy(String raw) {
      return this.toLegacy(this.deserialize(raw));
   }

   public String toLegacy(Component component) {
      return this.legacySection.serialize((Component)(component == null ? Component.empty() : component));
   }

   public List<String> toLegacyList(List<String> raw) {
      return raw.stream().map(this::toLegacy).collect(Collectors.toList());
   }

   public Component deserialize(String input) {
      return this.parse(input);
   }

   private String replaceVars(String line, Map<String, String> vars) {
      String replaced = line;

      for (Map.Entry<String, String> entry : vars.entrySet()) {
         replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue());
      }

      return replaced;
   }

   private Component parse(String input) {
      if (input == null) {
         return Component.empty();
      } else {
         String normalized = HEX_PATTERN.matcher(input).replaceAll("<#$1>");
         if (MINI_TAG_PATTERN.matcher(normalized).find()) {
            try {
               return this.miniMessage.deserialize(normalized);
            } catch (ParsingException var4) {
               this.plugin.getLogger().warning("Invalid MiniMessage format: " + input);
               return Component.text(input);
            }
         } else if (normalized.indexOf(167) >= 0) {
            return this.legacySection.deserialize(normalized);
         } else {
            return normalized.indexOf(38) >= 0 ? this.legacyAmpersand.deserialize(normalized) : Component.text(normalized);
         }
      }
   }
}
