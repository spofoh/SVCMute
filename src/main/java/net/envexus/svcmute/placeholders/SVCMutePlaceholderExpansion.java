package net.envexus.svcmute.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.envexus.svcmute.integrations.IntegrationManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SVCMutePlaceholderExpansion extends PlaceholderExpansion {
    private final IntegrationManager integrationManager;

    public SVCMutePlaceholderExpansion(IntegrationManager manager) {
        this.integrationManager = manager;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Envexus";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "svcmute";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        UUID uuid = offlinePlayer.getUniqueId();
        return switch (params) {
            case "muted" -> integrationManager.isPlayerMuted(uuid) ? "yes" : "no";
            case "remaining" -> integrationManager.getRemainingTime(uuid);
            case "remaining_ms" -> String.valueOf(integrationManager.getRemainingMilliseconds(uuid));
            default -> null;
        };
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of("muted", "remaining", "remaining_ms");
    }
}
