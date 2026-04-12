package net.envexus.svcmute;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.envexus.svcmute.configuration.ConfigurationManager;
import net.envexus.svcmute.integrations.IntegrationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.UUID;

public class MuteCheckPlugin implements VoicechatPlugin {

    private final JavaPlugin plugin;
    private final IntegrationManager integrationManager;
    private final ConfigurationManager configManager;

    public MuteCheckPlugin(JavaPlugin plugin, IntegrationManager integrationManager, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.integrationManager = integrationManager;
        this.configManager = configManager;
    }

    @Override
    public String getPluginId() {
        return "mutecheck_voicechat";
    }

    @Override
    public void initialize(VoicechatApi api) {}

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone, 0);
    }

    public void onMicrophone(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;

        UUID uuid = event.getSenderConnection().getPlayer().getUuid();
        IntegrationManager.CachedMute mute = integrationManager.getMute(uuid);

        if (mute != null) {
            if (mute.unmuteTime == -1 || mute.unmuteTime > System.currentTimeMillis()) {
                event.cancel();

                if (mute.shouldNotify()) {
                    UniversalScheduler.getScheduler(plugin).runTask(() -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            String remaining = (mute.unmuteTime == -1) ? "Permanent" : IntegrationManager.formatTime(mute.unmuteTime - System.currentTimeMillis());
                            String reason = mute.reason != null ? mute.reason : "No reason provided";

                            if (configManager.getConfig().getBoolean("actionbar", true)) {
                                player.sendActionBar(configManager.getAdvancedMessage("actionbar.muted",
                                        Placeholder.unparsed("reason", reason),
                                        Placeholder.unparsed("remaining_time", remaining)));
                            }
                            if (configManager.getConfig().getBoolean("message", true)) {
                                player.sendMessage(configManager.getAdvancedMessage("messages.muted",
                                        Placeholder.unparsed("reason", reason),
                                        Placeholder.unparsed("remaining_time", remaining)));
                            }
                        }
                    });
                }
            } else {
                integrationManager.removeMutedPlayer(uuid);
            }
        }
    }
}