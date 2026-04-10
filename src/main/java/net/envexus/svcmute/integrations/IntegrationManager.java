package net.envexus.svcmute.integrations;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IntegrationManager {
    private final Map<UUID, CachedMute> activeMutes = new ConcurrentHashMap<>(1024);

    public boolean isPlayerMuted(UUID uuid) {
        CachedMute mute = activeMutes.get(uuid);
        if (mute == null) return false;

        if (mute.unmuteTime == -1) return true;

        if (mute.unmuteTime <= System.currentTimeMillis()) {
            activeMutes.remove(uuid);
            return false;
        }
        return true;
    }

    public void addMutedPlayer(UUID uuid, long unmuteTime, String reason) {
        activeMutes.put(uuid, new CachedMute(unmuteTime, reason));
    }

    public void removeMutedPlayer(UUID uuid) {
        activeMutes.remove(uuid);
    }

    public CachedMute getMute(UUID uuid) {
        return activeMutes.get(uuid);
    }

    public String getRemainingTime(UUID uuid) {
        CachedMute mute = activeMutes.get(uuid);
        if (mute == null) return "0s";
        if (mute.unmuteTime == -1) return "Permanent";
        return formatTime(mute.unmuteTime - System.currentTimeMillis());
    }

    public long getRemainingMilliseconds(UUID uuid) {
        CachedMute mute = activeMutes.get(uuid);
        if (mute == null) return 0;
        return Math.max(0, mute.unmuteTime - System.currentTimeMillis());
    }

    public static String formatTime(long remainingTime) {
        if (remainingTime == -1) return "Permanent";
        if (remainingTime <= 0) return "0s";
        long seconds = remainingTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + hours % 24 + "h";
        if (hours > 0) return hours + "h " + minutes % 60 + "m";
        if (minutes > 0) return minutes + "m " + seconds % 60 + "s";
        return seconds + "s";
    }

    public static class CachedMute {
        public final long unmuteTime;
        public final String reason;
        private long lastNotified = 0;

        public CachedMute(long unmuteTime, String reason) {
            this.unmuteTime = unmuteTime;
            this.reason = reason;
        }

        public boolean shouldNotify() {
            long now = System.currentTimeMillis();
            if (now - lastNotified > 3000) {
                lastNotified = now;
                return true;
            }
            return false;
        }
    }
}