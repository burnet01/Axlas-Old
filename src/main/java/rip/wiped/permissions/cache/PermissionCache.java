package rip.wiped.permissions.cache;

import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cache abstraction backed by either Redis (shared across proxy servers) or an in-process
 * {@link LocalCache} when Redis is unavailable. Caching is only an optimization; storage
 * (Mongo) remains the source of truth.
 */
public interface PermissionCache {

    // --- Player data ---

    Optional<PlayerData> getCachedPlayer(UUID uuid);

    void cachePlayer(PlayerData data);

    void invalidatePlayer(UUID uuid);

    // --- Rank ---

    Optional<Rank> getCachedRank(String name);

    void cacheRank(Rank rank);

    void invalidateRank(String name);

    // --- Resolved permissions (rank perms + extra - denied) ---

    Optional<Set<String>> getCachedPermissions(UUID uuid);

    void cachePermissions(UUID uuid, Set<String> permissions);

    void invalidatePermissions(UUID uuid);

    void invalidateAllPlayersForRank(String rankName);

    // --- Lifecycle ---

    boolean isAvailable();

    void close();
}