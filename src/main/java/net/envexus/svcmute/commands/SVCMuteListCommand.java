package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

    public SVCMuteListCommand(SQLiteHelper db, SVCMute plugin) {
        this.db = db;
        this.plugin = plugin;
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
                sender.sendMessage("§cNo active mutes found.");
                return;
            }

            int pageSize = 10;
            int maxPages = (int) Math.ceil((double) activeMutes.size() / pageSize);
            int currentPage = Math.max(1, Math.min(page, maxPages));
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, activeMutes.size());

            sender.sendMessage("§8=== §eSVCMute List (Page " + currentPage + "/" + maxPages + ") §8===");
            for (int i = start; i < end; i++) {
                SQLiteHelper.MuteEntry mute = activeMutes.get(i);
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(mute.uuid()));
                String name = player.getName() != null ? player.getName() : mute.uuid();
                String remaining = mute.unmuteTime() == -1 ? "§4Permanent" : "§e" + IntegrationManager.formatTime(mute.unmuteTime() - System.currentTimeMillis());

                sender.sendMessage("§7- §c" + name + " §7| " + remaining + " §7| By: §b" + mute.executor());
            }
            sendPaginationFooter(sender, currentPage, maxPages);
        });
    }

    private void sendPaginationFooter(CommandSender sender, int page, int maxPages) {
        MiniMessage mm = MiniMessage.miniMessage();
        StringBuilder footer = new StringBuilder("<dark_gray>=== ");
        if (page > 1) footer.append("<click:run_command:\"/svcmutelist ").append(page - 1).append("\"><red>[< Prev]</red></click> ");
        footer.append("<gray>Page <yellow>").append(page).append("</yellow> of ").append(maxPages).append(" ");
        if (page < maxPages) footer.append("<click:run_command:\"/svcmutelist ").append(page + 1).append("\"><green>[Next >]</green></click>");
        footer.append(" <dark_gray>===");
        sender.sendMessage(mm.deserialize(footer.toString()));
    }
}