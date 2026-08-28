package rip.wiped.permissions.essentials;

import rip.wiped.permissions.model.PunishmentData;
import rip.wiped.permissions.storage.MongoStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Punishment store backed by Mongo. Loaded into memory at startup and written
 * through on every change, so punishments persist across restarts.
 */
public final class PunishmentManager {

    private final MongoStorage mongo;
    private final Map<String, PunishmentData> cache = new ConcurrentHashMap<>();

    public PunishmentManager(MongoStorage mongo) {
        this.mongo = mongo;
        mongo.loadAllPunishments().forEach(cache::put);
    }

    public boolean ban(String name, String reason, long durationMillis) {
        String key = key(name);
        PunishmentData current = cache.getOrDefault(key, PunishmentData.EMPTY);
        if (current.banExpiresAt() > System.currentTimeMillis()) return false;
        put(key, new PunishmentData(
                System.currentTimeMillis() + durationMillis, reason,
                current.muteExpiresAt(), current.muteReason(), current.warnings()));
        return true;
    }

    public boolean unban(String name) {
        String key = key(name);
        PunishmentData current = cache.get(key);
        if (current == null || current.banExpiresAt() <= 0) return false;
        put(key, new PunishmentData(
                0L, null, current.muteExpiresAt(), current.muteReason(), current.warnings()));
        return true;
    }

    public boolean isBanned(String name) {
        PunishmentData data = cache.get(key(name));
        return data != null && data.banExpiresAt() > System.currentTimeMillis();
    }

    public String banReason(String name) {
        PunishmentData data = cache.get(key(name));
        return data != null && data.banExpiresAt() > System.currentTimeMillis() ? data.banReason() : null;
    }

    public boolean mute(String name, String reason, long durationMillis) {
        String key = key(name);
        PunishmentData current = cache.getOrDefault(key, PunishmentData.EMPTY);
        if (current.muteExpiresAt() > System.currentTimeMillis()) return false;
        put(key, new PunishmentData(
                current.banExpiresAt(), current.banReason(),
                System.currentTimeMillis() + durationMillis, reason, current.warnings()));
        return true;
    }

    public boolean unmute(String name) {
        String key = key(name);
        PunishmentData current = cache.get(key);
        if (current == null || current.muteExpiresAt() <= 0) return false;
        put(key, new PunishmentData(
                current.banExpiresAt(), current.banReason(), 0L, null, current.warnings()));
        return true;
    }

    public boolean isMuted(String name) {
        PunishmentData data = cache.get(key(name));
        return data != null && data.muteExpiresAt() > System.currentTimeMillis();
    }

    public void warn(String name) {
        String key = key(name);
        PunishmentData current = cache.getOrDefault(key, PunishmentData.EMPTY);
        put(key, new PunishmentData(
                current.banExpiresAt(), current.banReason(),
                current.muteExpiresAt(), current.muteReason(), current.warnings() + 1));
    }

    public int warningCount(String name) {
        PunishmentData data = cache.get(key(name));
        return data != null ? data.warnings() : 0;
    }

    private void put(String key, PunishmentData data) {
        cache.put(key, data);
        mongo.savePunishment(key, data);
    }

    private static String key(String name) {
        return name.toLowerCase();
    }
}