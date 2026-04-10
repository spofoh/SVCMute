package net.envexus.svcmute;

import co.aikar.commands.BukkitCommandManager;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import net.envexus.svcmute.commands.*;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.placeholders.SVCMutePlaceholderExpansion;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SVCMute extends JavaPlugin {
    private MuteCheckPlugin voicechatPlugin;
    private SQLiteHelper sqliteHelper;
    private IntegrationManager integrationManager;

    @Override
    public void onEnable() {
        ConfigurationManager configManager = new ConfigurationManager(this);

        sqliteHelper = new SQLiteHelper();
        integrationManager = new IntegrationManager();

        sqliteHelper.getAllActiveMutes().forEach(mute ->
                integrationManager.addMutedPlayer(UUID.fromString(mute.uuid()), mute.unmuteTime(), mute.reason())
        );

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            voicechatPlugin = new MuteCheckPlugin(this, integrationManager, configManager);
            service.registerPlugin(voicechatPlugin);
        }

        if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SVCMutePlaceholderExpansion(integrationManager).register();
        }

        BukkitCommandManager manager = new BukkitCommandManager(this);

        manager.getCommandCompletions().registerAsyncCompletion("mutedplayers", c ->
                sqliteHelper.getAllActiveMutes().stream()
                        .map(mute -> getServer().getOfflinePlayer(UUID.fromString(mute.uuid())).getName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));

        manager.registerCommand(new SVCMuteCommand(sqliteHelper, this, integrationManager, configManager));
        manager.registerCommand(new SVCUnmuteCommand(sqliteHelper, this, integrationManager, configManager));
        manager.registerCommand(new SVCMuteListCommand(sqliteHelper, this, configManager));
        manager.registerCommand(new SVCHistoryCommand(sqliteHelper, this, configManager));
        manager.registerCommand(new SVCReloadCommand(configManager));
    }

    @Override
    public void onDisable() {
        if (voicechatPlugin != null) {
            getServer().getServicesManager().unregister(voicechatPlugin);
        }
    }
}