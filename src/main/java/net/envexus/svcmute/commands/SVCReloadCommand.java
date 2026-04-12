package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import net.envexus.svcmute.configuration.ConfigurationManager;
import org.bukkit.command.CommandSender;

@CommandAlias("svcreload")
@CommandPermission("svcmute.reload")
public class SVCReloadCommand extends BaseCommand {

    private final ConfigurationManager configManager;

    public SVCReloadCommand(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Default
    public void onReload(CommandSender sender) {
        configManager.loadFiles();
        sender.sendMessage(configManager.getMessage("reload.success"));
    }
}