package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
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

    public SVCMuteCommand(SQLiteHelper db, SVCMute plugin, IntegrationManager integrationManager) {
        this.db = db;
        this.plugin = plugin;
        this.integrationManager = integrationManager;
    }

    @Default
    @Syntax("<player> <time> [reason]")
    @CommandCompletion("@players perm|1h|1d|30d [reason]")
    public void onMute(CommandSender sender, String playerName, String timeStr, @Optional @Default("No reason") String reason) {
        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = target.getUniqueId();

            long unmuteTime;
            String durationDisplay;

            if (timeStr.equalsIgnoreCase("perm") || timeStr.equalsIgnoreCase("permanent")) {
                unmuteTime = -1;
                durationDisplay = "Permanent";
            } else {
                long duration = parseTime(timeStr);
                if (duration <= 0) {
                    sender.sendMessage("§cInvalid time format. Use 'perm' or e.g., '10m', '1h', '7d'.");
                    return;
                }
                unmuteTime = System.currentTimeMillis() + duration;
                durationDisplay = timeStr;
            }

            db.addMute(uuid.toString(), unmuteTime, reason, sender.getName());
            db.addMuteHistory(uuid.toString(), System.currentTimeMillis(), durationDisplay, reason, sender.getName());
            integrationManager.addMutedPlayer(uuid, unmuteTime, reason);

            sender.sendMessage("§aMuted " + (target.getName() != null ? target.getName() : playerName) + " for " + timeStr);
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