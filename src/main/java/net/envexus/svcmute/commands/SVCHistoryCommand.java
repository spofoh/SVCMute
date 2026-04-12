package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
    private final ConfigurationManager config;
    private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("dd.MM.yyyy HH:mm"));

    public SVCHistoryCommand(SQLiteHelper db, SVCMute plugin, ConfigurationManager config) {
        this.db = db;
        this.plugin = plugin;
        this.config = config;
    }

    @Default
    @Syntax("<player> [page]")
    @CommandCompletion("@players")
    public void onHistory(CommandSender sender, String playerName, @Default("1") int pageInput) {
        UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            List<SQLiteHelper.HistoryEntry> history = db.getMuteHistory(player.getUniqueId().toString());

            if (history == null || history.isEmpty()) {
                sender.sendMessage(config.getMessage("messages.no_history", "player", playerName));
                return;
            }

            int pageSize = 5;
            int maxPages = (int) Math.ceil((double) history.size() / pageSize);
            int page = Math.max(1, Math.min(pageInput, maxPages));
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, history.size());

            sender.sendMessage(config.getMessage("messages.history_header", "player", player.getName(), "current_page", String.valueOf(page), "max_pages", String.valueOf(maxPages)));

            for (int i = start; i < end; i++) {
                SQLiteHelper.HistoryEntry entry = history.get(i);
                String dateStr = dateFormat.get().format(new Date(entry.timestamp()));
                int entryNumber = history.size() - i;

                sender.sendMessage(config.getMessage("messages.history_entry",
                        "entry_number", String.valueOf(entryNumber),
                        "date", dateStr,
                        "executor", entry.executor(),
                        "duration", entry.duration(),
                        "reason", entry.reason()
                ));

                if (i < end - 1) {
                    sender.sendMessage(Component.empty());
                }
            }

            String prevLabelRaw = page > 1 ? config.getRawString("pagination.previous_active") : config.getRawString("pagination.previous_inactive");
            String nextLabelRaw = page < maxPages ? config.getRawString("pagination.next_active") : config.getRawString("pagination.next_inactive");

            Component prevBtn = MiniMessage.miniMessage().deserialize(prevLabelRaw);
            Component nextBtn = MiniMessage.miniMessage().deserialize(nextLabelRaw);

            if (page > 1) {
                prevBtn = prevBtn
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/svchistory " + playerName + " " + (page - 1)))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(MiniMessage.miniMessage().deserialize("<gray>Click to go to page " + (page - 1))));
            }

            if (page < maxPages) {
                nextBtn = nextBtn
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/svchistory " + playerName + " " + (page + 1)))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(MiniMessage.miniMessage().deserialize("<gray>Click to go to page " + (page + 1))));
            }

            sender.sendMessage(config.getAdvancedMessage("messages.history_footer",
                    Placeholder.unparsed("player", playerName),
                    Placeholder.unparsed("current_page", String.valueOf(page)),
                    Placeholder.unparsed("max_pages", String.valueOf(maxPages)),
                    Placeholder.component("prev_button", prevBtn),
                    Placeholder.component("next_button", nextBtn)
            ));
        });
    }
}