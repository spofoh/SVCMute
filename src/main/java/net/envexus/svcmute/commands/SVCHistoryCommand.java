package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.util.SQLiteHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CommandAlias("svchistory")
@CommandPermission("svcmute.history")
public class SVCHistoryCommand extends BaseCommand {

    private final SQLiteHelper db;
    private final SVCMute plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("dd.MM.yyyy HH:mm"));

    public SVCHistoryCommand(SQLiteHelper db, SVCMute plugin) {
        this.db = db;
        this.plugin = plugin;
    }

    @Default
    @Syntax("<player> [page]")
    @CommandCompletion("@players")
    public void onHistory(CommandSender sender, String playerName, @Default("1") int pageInput) {
        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            List<SQLiteHelper.HistoryEntry> history = db.getMuteHistory(player.getUniqueId().toString());

            if (history == null || history.isEmpty()) {
                sender.sendMessage(mm.deserialize("<red>No history found for <white>" + playerName));
                return;
            }

            int pageSize = 5;
            int maxPages = (int) Math.ceil((double) history.size() / pageSize);
            int page = Math.max(1, Math.min(pageInput, maxPages));
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, history.size());

            sender.sendMessage(mm.deserialize("<newline><yellow>History for <white>" + player.getName() + " <gray>(Page " + page + "/" + maxPages + ")"));
            sender.sendMessage(mm.deserialize("<dark_gray><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</st>"));

            for (int i = start; i < end; i++) {
                SQLiteHelper.HistoryEntry entry = history.get(i);
                String dateStr = dateFormat.get().format(new Date(entry.timestamp()));
                int entryNumber = history.size() - i;

                sender.sendMessage(mm.deserialize(
                        "<gold>#" + entryNumber + " <gray>[" + dateStr + "] <dark_gray>| <aqua>" + entry.executor() + " <dark_gray>| <green>" + entry.duration()
                ));

                sender.sendMessage(mm.deserialize(
                        "  <white>Reason: <gray>" + entry.reason()
                ));

                if (i < end - 1) {
                    sender.sendMessage(Component.empty());
                }
            }

            sender.sendMessage(mm.deserialize("<dark_gray><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</st>"));

            Component pagination = Component.empty();

            if (page > 1) {
                pagination = pagination.append(mm.deserialize("<gold><bold>[« Previous]</bold>")
                        .clickEvent(ClickEvent.runCommand("/svchistory " + playerName + " " + (page - 1)))
                        .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>Click for page " + (page - 1)))));
            } else {
                pagination = pagination.append(mm.deserialize("<dark_gray><bold>[« Previous]</bold>"));
            }

            pagination = pagination.append(mm.deserialize("  <gray>|  "));

            if (page < maxPages) {
                pagination = pagination.append(mm.deserialize("<gold><bold>[Next »]</bold>")
                        .clickEvent(ClickEvent.runCommand("/svchistory " + playerName + " " + (page + 1)))
                        .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>Click for page " + (page + 1)))));
            } else {
                pagination = pagination.append(mm.deserialize("<dark_gray><bold>[Next »]</bold>"));
            }

            sender.sendMessage(pagination.append(Component.newline()));
        });
    }
}