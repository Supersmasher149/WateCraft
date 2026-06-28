package com.wally.kissmod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestManager {
    public static final long REQUEST_COOLDOWN_MS = 2000L;

    private static final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public static boolean hasPendingRequest(UUID targetUUID) {
        return pendingRequests.containsKey(targetUUID);
    }

    public static UUID getRequester(UUID targetUUID) {
        return pendingRequests.get(targetUUID);
    }

    public static boolean createRequest(UUID requester, UUID target) {
        return pendingRequests.putIfAbsent(target, requester) == null;
    }

    public static void removeRequest(UUID targetUUID) {
        pendingRequests.remove(targetUUID);
    }

    public static boolean isOnCooldown(UUID playerUUID) {
        Long endTime = cooldowns.get(playerUUID);
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(playerUUID);
            return false;
        }
        return true;
    }

    public static void setCooldown(UUID playerUUID) {
        cooldowns.put(playerUUID, System.currentTimeMillis() + REQUEST_COOLDOWN_MS);
    }

    public static long getCooldownRemaining(UUID playerUUID) {
        Long endTime = cooldowns.get(playerUUID);
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public static void removeAllForPlayer(UUID playerUUID) {
        pendingRequests.values().removeIf(requester -> requester.equals(playerUUID));
        pendingRequests.entrySet().removeIf(entry -> entry.getKey().equals(playerUUID));
        cooldowns.remove(playerUUID);
    }
}
