package net.envexus.svcmute.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.envexus.svcmute.SVCMute;
import net.envexus.svcmute.integrations.IntegrationManager;
import net.envexus.svcmute.util.SQLiteHelper;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
        String prevLabelRaw = page > 1 ? config.getRawString("pagination.previous_active") : config.getRawString("pagination.previous_inactive");
        String nextLabelRaw = page < maxPages ? config.getRawString("pagination.next_active") : config.getRawString("pagination.next_inactive");

        Component prevBtn = MiniMessage.miniMessage().deserialize(prevLabelRaw);
        Component nextBtn = MiniMessage.miniMessage().deserialize(nextLabelRaw);

        if (page > 1) {
            prevBtn = prevBtn
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/svcmutelist " + (page - 1)))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Go to page " + (page - 1))));
        }

        if (page < maxPages) {
            nextBtn = nextBtn
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/svcmutelist " + (page + 1)))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Go to page " + (page + 1))));
        }

        sender.sendMessage(config.getAdvancedMessage("messages.mute_list_footer",
                Placeholder.unparsed("current_page", String.valueOf(page)),
                Placeholder.unparsed("max_pages", String.valueOf(maxPages)),
                Placeholder.component("prev_button", prevBtn),
                Placeholder.component("next_button", nextBtn)
        ));
    }
}