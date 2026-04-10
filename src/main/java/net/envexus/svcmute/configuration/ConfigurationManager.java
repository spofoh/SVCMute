package net.envexus.svcmute.configuration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigurationManager {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration messagesConfig;
    private FileConfiguration pluginConfig;

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        loadFiles();
    }

    public void loadFiles() {
        this.messagesConfig = loadConfiguration("locale.yml");
        this.pluginConfig = loadConfiguration("config.yml");
    }

    private FileConfiguration loadConfiguration(String filename) {
        File messagesFile = new File(plugin.getDataFolder(), filename);

        if (!messagesFile.exists()) {
            plugin.saveResource(filename, false);
        }

        return YamlConfiguration.loadConfiguration(messagesFile);
    }

    public Component getLocaleString(String key, TagResolver... resolvers) {
        String rawMessage = messagesConfig.getString(
                key,
                "<red>Message not found for key: %s</red>".formatted(key)
        );

        TagResolver prefixResolver = Placeholder.component(
                "prefix",
                miniMessage.deserialize(messagesConfig.getString("prefix", ""))
        );

        TagResolver combined;
        if (resolvers.length == 0) {
            combined = prefixResolver;
        } else {
            combined = TagResolver.builder()
                    .resolver(prefixResolver)
                    .resolvers(resolvers)
                    .build();
        }

        return miniMessage.deserialize(rawMessage, combined);
    }

    public FileConfiguration getConfig() {
        return pluginConfig;
    }
}
