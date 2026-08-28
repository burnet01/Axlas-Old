package rip.wiped.permissions.service;

import rip.wiped.permissions.cache.PermissionCache;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;
import rip.wiped.permissions.storage.MongoStorage;
import rip.wiped.permissions.trie.PermissionTrie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public final class PermissionService {

    private static final Logger LOGGER = Logger.getLogger("AxlasPermissions");
    private static final String DEFAULT_RANK = "Default";

    private final MongoStorage mongo;
    private final PermissionCache redis;
    private final PermissionTrie globalTrie;
    private final Map<String, Rank> rankCache = new ConcurrentHashMap<>();
    private final List<Runnable> rankChangeListeners = new CopyOnWriteArrayList<>();
    private String defaultRankName = DEFAULT_RANK;

    public PermissionService(MongoStorage mongo, PermissionCache redis) {
        this.mongo = mongo;
        this.redis = redis;
        this.globalTrie = new PermissionTrie();
        loadRanks();
    }

    // --- Initialization ---

    public void loadRanks() {
        rankCache.clear();
        rankCache.putAll(mongo.loadAllRanks());
        rebuildGlobalTrie();
        ensureDefaultRank();
        LOGGER.info("Loaded " + rankCache.size() + " ranks into trie (" + globalTrie.size() + " nodes)");
    }

    private void ensureDefaultRank() {
        if (!rankCache.containsKey(defaultRankName)) {
            Rank defaultRank = new Rank(defaultRankName, 0, "&7", "&7", Set.of(
                    "minecraft.command.help",
                    "minecraft.command.list"
            ));
            mongo.saveRank(defaultRank);
            rankCache.put(defaultRankName, defaultRank);
            redis.cacheRank(defaultRank);
            LOGGER.info("Created default rank: " + defaultRankName);
        }
    }

    private void rebuildGlobalTrie() {
        globalTrie.clear();
        for (Rank rank : rankCache.values()) {
            for (String perm : rank.permissions()) {
                globalTrie.insert(perm);
            }
        }
    }

    // --- Player Operations ---

    public PlayerData getOrCreatePlayer(UUID uuid, String username) {
        Optional<PlayerData> cached = redis.getCachedPlayer(uuid);
        if (cached.isPresent()) return cached.get();

        Optional<PlayerData> fromMongo = mongo.loadPlayer(uuid);
        if (fromMongo.isPresent()) {
            PlayerData data = fromMongo.get();
            redis.cachePlayer(data);
            return data;
        }

        PlayerData newPlayer = new PlayerData(uuid, username, defaultRankName, Set.of(), Set.of());
        mongo.createDefaultPlayer(uuid, username, defaultRankName);
        redis.cachePlayer(newPlayer);
        return newPlayer;
    }

    public void setPlayerRank(UUID uuid, String rankName) {
        Optional<PlayerData> opt = mongo.loadPlayer(uuid);
        if (opt.isEmpty()) return;
        PlayerData old = opt.get();
        if (!rankCache.containsKey(rankName)) return;

        PlayerData updated = new PlayerData(old.uuid(), old.username(), rankName, old.extraPermissions(), old.deniedPermissions());
        mongo.savePlayer(updated);
        redis.cachePlayer(updated);
        redis.invalidatePermissions(uuid);
    }

    public void addExtraPermission(UUID uuid, String permission) {
        Optional<PlayerData> opt = mongo.loadPlayer(uuid);
        if (opt.isEmpty()) return;
        PlayerData old = opt.get();
        Set<String> newExtra = new HashSet<>(old.extraPermissions());
        newExtra.add(permission);

        PlayerData updated = new PlayerData(old.uuid(), old.username(), old.rankName(), newExtra, old.deniedPermissions());
        mongo.savePlayer(updated);
        redis.cachePlayer(updated);
        redis.invalidatePermissions(uuid);
    }

    public void removeExtraPermission(UUID uuid, String permission) {
        Optional<PlayerData> opt = mongo.loadPlayer(uuid);
        if (opt.isEmpty()) return;
        PlayerData old = opt.get();
        Set<String> newExtra = new HashSet<>(old.extraPermissions());
        newExtra.remove(permission);

        PlayerData updated = new PlayerData(old.uuid(), old.username(), old.rankName(), newExtra, old.deniedPermissions());
        mongo.savePlayer(updated);
        redis.cachePlayer(updated);
        redis.invalidatePermissions(uuid);
    }

    public void denyPermission(UUID uuid, String permission) {
        Optional<PlayerData> opt = mongo.loadPlayer(uuid);
        if (opt.isEmpty()) return;
        PlayerData old = opt.get();
        Set<String> newDenied = new HashSet<>(old.deniedPermissions());
        newDenied.add(permission);

        PlayerData updated = new PlayerData(old.uuid(), old.username(), old.rankName(), old.extraPermissions(), newDenied);
        mongo.savePlayer(updated);
        redis.cachePlayer(updated);
        redis.invalidatePermissions(uuid);
    }

    public void unDenyPermission(UUID uuid, String permission) {
        Optional<PlayerData> opt = mongo.loadPlayer(uuid);
        if (opt.isEmpty()) return;
        PlayerData old = opt.get();
        Set<String> newDenied = new HashSet<>(old.deniedPermissions());
        newDenied.remove(permission);

        PlayerData updated = new PlayerData(old.uuid(), old.username(), old.rankName(), old.extraPermissions(), newDenied);
        mongo.savePlayer(updated);
        redis.cachePlayer(updated);
        redis.invalidatePermissions(uuid);
    }

    // --- Permission Checking (the hot path) ---

    /**
     * Check if a player has a specific permission. Uses resolved cache first,
     * then falls back to trie lookup + extra/denied.
     */
    public boolean hasPermission(UUID uuid, String permission) {
        // 1. Check resolved permissions cache
        Optional<Set<String>> cachedPerms = redis.getCachedPermissions(uuid);
        if (cachedPerms.isPresent()) {
            return hasResolved(cachedPerms.get(), permission);
        }

        // 2. Resolve from stored data without writing anything on a read path
        Set<String> resolved = mongo.loadPlayer(uuid)
                .map(this::resolvePermissions)
                .orElseGet(this::defaultPermissions);
        redis.cachePermissions(uuid, resolved);

        return hasResolved(resolved, permission);
    }

    /**
     * Check if a player has a permission (fast path using pre-resolved set).
     */
    public boolean hasPermission(PlayerData player, String permission) {
        return hasResolved(resolvePermissions(player), permission);
    }

    private boolean hasResolved(Set<String> resolved, String permission) {
        if (resolved.contains(permission) || resolved.contains("*")) return true;
        return hasWildcardMatch(resolved, permission);
    }

    private Set<String> defaultPermissions() {
        Rank rank = rankCache.get(defaultRankName);
        return rank != null ? rank.permissions() : Set.of();
    }

    /**
     * Resolve a player's full permission set: rank permissions + extra - denied.
     */
    public Set<String> resolvePermissions(PlayerData player) {
        Set<String> resolved = new HashSet<>();

        // Add rank permissions
        Rank rank = rankCache.get(player.rankName());
        if (rank != null) {
            resolved.addAll(rank.permissions());
        }

        // Add extra permissions
        resolved.addAll(player.extraPermissions());

        // Remove denied permissions
        resolved.removeAll(player.deniedPermissions());

        return resolved;
    }

    /**
     * Check if a resolved set has a wildcard match for the given permission.
     * e.g., resolved has "foo.*" and permission is "foo.bar" → true.
     */
    private boolean hasWildcardMatch(Set<String> resolved, String permission) {
        String[] segments = permission.split("\\.");
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            if (!prefix.isEmpty()) prefix.append(".");
            prefix.append(segments[i]);
            String wildcard = prefix + ".*";
            if (resolved.contains(wildcard)) return true;
        }
        return false;
    }

    // --- Rank Operations ---

    public Optional<Rank> getRank(String name) {
        Rank cached = rankCache.get(name);
        if (cached != null) return Optional.of(cached);

        Optional<Rank> fromRedis = redis.getCachedRank(name);
        if (fromRedis.isPresent()) {
            rankCache.put(name, fromRedis.get());
            return fromRedis;
        }

        Optional<Rank> fromMongo = mongo.loadRank(name);
        fromMongo.ifPresent(rank -> {
            rankCache.put(name, rank);
            redis.cacheRank(rank);
        });
        return fromMongo;
    }

    public void createRank(Rank rank) {
        mongo.saveRank(rank);
        rankCache.put(rank.name(), rank);
        redis.cacheRank(rank);
        rebuildGlobalTrie();
        // Invalidate all cached permissions since trie changed
        invalidateAllPermissions();
    }

    public void updateRank(Rank rank) {
        mongo.saveRank(rank);
        rankCache.put(rank.name(), rank);
        redis.cacheRank(rank);
        redis.invalidateRank(rank.name());
        rebuildGlobalTrie();
        invalidateAllPermissions();
        notifyRankChange();
    }

    public void deleteRank(String name) {
        mongo.deleteRank(name);
        rankCache.remove(name);
        redis.invalidateRank(name);
        rebuildGlobalTrie();
        invalidateAllPermissions();
        notifyRankChange();
    }

    public void addRankPermission(String rankName, String permission) {
        Rank rank = rankCache.get(rankName);
        if (rank == null) return;
        Set<String> newPerms = new HashSet<>(rank.permissions());
        newPerms.add(permission);
        Rank updated = new Rank(rank.name(), rank.weight(), rank.prefix(), rank.color(), newPerms);
        updateRank(updated);
    }

    public void removeRankPermission(String rankName, String permission) {
        Rank rank = rankCache.get(rankName);
        if (rank == null) return;
        Set<String> newPerms = new HashSet<>(rank.permissions());
        newPerms.remove(permission);
        Rank updated = new Rank(rank.name(), rank.weight(), rank.prefix(), rank.color(), newPerms);
        updateRank(updated);
    }

    public Optional<PlayerData> getPlayerByName(String username) {
        return mongo.loadPlayerByName(username);
    }

    public Map<String, Rank> getAllRanks() {
        return Collections.unmodifiableMap(rankCache);
    }

    // --- Bulk Operations ---

    private void invalidateAllPermissions() {
        redis.invalidateAllPlayersForRank("*");
    }

    // --- Rank Change Listeners ---

    public void onRankChange(Runnable listener) {
        rankChangeListeners.add(listener);
    }

    private void notifyRankChange() {
        for (Runnable listener : rankChangeListeners) {
            listener.run();
        }
    }

    // --- Accessors ---

    public MongoStorage mongoStorage() {
        return mongo;
    }

    public PermissionTrie globalTrie() {
        return globalTrie;
    }

    public String defaultRankName() {
        return defaultRankName;
    }
}
