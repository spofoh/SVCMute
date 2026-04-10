package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("svcunmute")
@CommandPermission("svcmute.unmute")
public class SCVUnmuteCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final IntegrationManager integrationManager;

    public SCVUnmuteCommand(SQLiteHelper db, SVCMute plugin, IntegrationManager integrationManager) {
        this.db = db;
        this.plugin = plugin;
        this.integrationManager = integrationManager;
    }

    @Default
    @Syntax("<player>")
    @CommandCompletion("@mutedplayers")
    public void onUnmute(CommandSender sender, String playerName) {
        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            String uuidStr = player.getUniqueId().toString();

            if (!db.isMuted(uuidStr)) {
                sender.sendMessage("§c" + playerName + " is not muted.");
                return;
            }

            db.removeMute(uuidStr);
            integrationManager.removeMutedPlayer(player.getUniqueId());
            sender.sendMessage("§a" + playerName + " has been unmuted.");
        });
    }
}