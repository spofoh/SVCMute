package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import com.github.Anon8281.universalScheduler.UniversalScheduler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CommandAlias("svcmute")
@CommandPermission("svcmute.mute")
public class SVCMuteCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final IntegrationManager integrationManager;
    private final ConfigurationManager config;

    public SVCMuteCommand(SQLiteHelper db, SVCMute plugin, IntegrationManager integrationManager, ConfigurationManager config) {
        this.db = db;
        this.plugin = plugin;
        this.integrationManager = integrationManager;
        this.config = config;
    }

    @Default
    @Syntax("<player> <time> [reason]")
    @CommandCompletion("@players perm|1h|1d|30d [reason]")
    public void onMute(CommandSender sender, String playerName, String timeStr, @Optional String[] reasonArgs) {
        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = target.getUniqueId();

            long unmuteTime;
            String durationDisplay;
            String reason = (reasonArgs == null || reasonArgs.length == 0) ? "No reason" : String.join(" ", reasonArgs);

            if (timeStr.equalsIgnoreCase("perm") || timeStr.equalsIgnoreCase("permanent")) {
                unmuteTime = -1;
                durationDisplay = "Permanent";
            } else {
                long duration = parseTime(timeStr);
                if (duration <= 0) {
                    sender.sendMessage(config.getMessage("messages.invalid_time_format"));
                    return;
                }
                unmuteTime = System.currentTimeMillis() + duration;
                durationDisplay = timeStr;
            }

            db.addMute(uuid.toString(), unmuteTime, reason, sender.getName());
            db.addMuteHistory(uuid.toString(), System.currentTimeMillis(), durationDisplay, reason, sender.getName());
            integrationManager.addMutedPlayer(uuid, unmuteTime, reason);

            sender.sendMessage(config.getMessage("messages.mute_success",
                    "player", target.getName() != null ? target.getName() : playerName,
                    "remaining_time", timeStr,
                    "reason", reason));
        });
    }

    private long parseTime(String timeStr) {
        try {
            char unit = timeStr.toLowerCase().charAt(timeStr.length() - 1);
            long amount = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(amount);
                case 'm' -> TimeUnit.MINUTES.toMillis(amount);
                case 'h' -> TimeUnit.HOURS.toMillis(amount);
                case 'd' -> TimeUnit.DAYS.toMillis(amount);
                default -> -1;
            };
        } catch (Exception e) { return -1; }
    }
}