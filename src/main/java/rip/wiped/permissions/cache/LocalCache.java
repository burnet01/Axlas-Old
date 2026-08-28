package rip.wiped.permissions.cache;

import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-dependency, in-process fallback cache used when Redis is unavailable.
 * Mirrors {@link RedisCache}'s semantics with TTL-based expiry on read.
 * Storage (Mongo) remains the source of truth.
 */
public final class LocalCache implements PermissionCache {

    private static final String PLAYER_PREFIX = "axlas:player:";
    private static final String RANK_PREFIX = "axlas:rank:";
    private static final String PERMS_PREFIX = "axlas:perms:";
    private static final long PLAYER_TTL_MILLIS = 300_000;   // 5 min
    private static final long RANK_TTL_MILLIS = 600_000;     // 10 min
    private static final long PERMS_TTL_MILLIS = 300_000;    // 5 min

    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<PlayerData> getCachedPlayer(UUID uuid) {
        return Optional.ofNullable((PlayerData) get(PLAYER_PREFIX + uuid));
    }

    @Override
    public void cachePlayer(PlayerData data) {
        put(PLAYER_PREFIX + data.uuid(), data, PLAYER_TTL_MILLIS);
    }

    @Override
    public void invalidatePlayer(UUID uuid) {
        entries.remove(PLAYER_PREFIX + uuid);
        entries.remove(PERMS_PREFIX + uuid);
    }

    @Override
    public Optional<Rank> getCachedRank(String name) {
        return Optional.ofNullable((Rank) get(RANK_PREFIX + name));
    }

    @Override
    public void cacheRank(Rank rank) {
        put(RANK_PREFIX + rank.name(), rank, RANK_TTL_MILLIS);
    }

    @Override
    public void invalidateRank(String name) {
        entries.remove(RANK_PREFIX + name);
    }

    @Override
    public Optional<Set<String>> getCachedPermissions(UUID uuid) {
        @SuppressWarnings("unchecked")
        Set<String> perms = (Set<String>) get(PERMS_PREFIX + uuid);
        return perms == null || perms.isEmpty() ? Optional.empty() : Optional.of(perms);
    }

    @Override
    public void cachePermissions(UUID uuid, Set<String> permissions) {
        put(PERMS_PREFIX + uuid, permissions, PERMS_TTL_MILLIS);
    }

    @Override
    public void invalidatePermissions(UUID uuid) {
        entries.remove(PERMS_PREFIX + uuid);
    }

    @Override
    public void invalidateAllPlayersForRank(String rankName) {
        entries.keySet().removeIf(key -> key.startsWith(PERMS_PREFIX));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void close() {
        entries.clear();
    }

    private Object get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) return null;
        if (entry.expiresAt() < System.currentTimeMillis()) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    private void put(String key, Object value, long ttlMillis) {
        entries.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlMillis));
    }

    private record CacheEntry(Object value, long expiresAt) {}
}