package net.envexus.svcmute.configuration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    private Component render(String key, TagResolver... resolvers) {
        String raw = messagesConfig.getString(key, "<red>Message key not found: " + key);

        TagResolver prefixResolver = Placeholder.component("prefix",
                miniMessage.deserialize(messagesConfig.getString("prefix", "")));

        return miniMessage.deserialize(raw, TagResolver.builder()
                .resolver(prefixResolver)
                .resolvers(resolvers)
                .build());
    }

    public Component getMessage(String key, String... replacements) {
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                resolvers.add(Placeholder.unparsed(replacements[i], replacements[i + 1]));
            }
        }
        return render(key, resolvers.toArray(new TagResolver[0]));
    }

    public Component getAdvancedMessage(String key, TagResolver... resolvers) {
        return render(key, resolvers);
    }

    public String getRawString(String key) {
        return messagesConfig.getString(key, "");
    }

    public FileConfiguration getConfig() {
        return pluginConfig;
    }
}
