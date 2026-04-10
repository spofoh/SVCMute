package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.configuration.ConfigurationManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

@CommandAlias("svcmutelist")
@CommandPermission("svcmute.mutelist")
public class SVCMuteListCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final ConfigurationManager config;

    public SVCMuteListCommand(SQLiteHelper db, SVCMute plugin, ConfigurationManager config) {
        this.db = db;
        this.plugin = plugin;
        this.config = config;
    }

    @Default
    @Syntax("[page]")
    public void onMuteList(CommandSender sender, String[] args) {
        int pageInput = 1;
        if (args.length > 0) {
            try { pageInput = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        final int page = pageInput;

        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            List<SQLiteHelper.MuteEntry> activeMutes = db.getAllActiveMutes();
            if (activeMutes.isEmpty()) {
                sender.sendMessage(config.getMessage("messages.no_active_mutes"));
                return;
            }

            int pageSize = 10;
            int maxPages = (int) Math.ceil((double) activeMutes.size() / pageSize);
            int currentPage = Math.max(1, Math.min(page, maxPages));
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, activeMutes.size());

            sender.sendMessage(config.getMessage("messages.mute_list_header", "current_page", String.valueOf(currentPage), "max_pages", String.valueOf(maxPages)));
            for (int i = start; i < end; i++) {
                SQLiteHelper.MuteEntry mute = activeMutes.get(i);
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(mute.uuid()));
                String name = player.getName() != null ? player.getName() : mute.uuid();
                String remaining = mute.unmuteTime() == -1 ? "Permanent" : IntegrationManager.formatTime(mute.unmuteTime() - System.currentTimeMillis());

                sender.sendMessage(config.getMessage("messages.mute_list_entry", "player", name, "remaining_time", remaining, "executor", mute.executor()));
            }
            sendPaginationFooter(sender, currentPage, maxPages);
        });
    }

    private void sendPaginationFooter(CommandSender sender, int page, int maxPages) {
        String prev = page > 1 ? "<click:run_command:\"/svcmutelist " + (page - 1) + "\"><red>[« Previous]</red></click>" : "<dark_gray><bold>[« Previous]</bold></dark_gray>";
        String next = page < maxPages ? "<click:run_command:\"/svcmutelist " + (page + 1) + "\"><green>[Next »]</green></click>" : "<dark_gray><bold>[Next »]</bold></dark_gray>";

        sender.sendMessage(config.getMessage("messages.mute_list_footer", "current_page", String.valueOf(page), "max_pages", String.valueOf(maxPages), "prev_button", prev, "next_button", next));
    }
}