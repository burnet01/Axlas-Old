package rip.wiped.permissions.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisCache implements PermissionCache {

    private static final Logger LOGGER = Logger.getLogger("AxlasPermissions");
    private static final String PLAYER_PREFIX = "axlas:player:";
    private static final String RANK_PREFIX = "axlas:rank:";
    private static final String PERMS_PREFIX = "axlas:perms:";
    private static final int PLAYER_CACHE_TTL_SECONDS = 300;   // 5 min
    private static final int RANK_CACHE_TTL_SECONDS = 600;     // 10 min
    private static final int PERMS_CACHE_TTL_SECONDS = 300;    // 5 min

    private final JedisPool pool;

    public RedisCache(String host, int port, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(32);
        config.setMaxIdle(16);
        config.setMinIdle(4);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        if (password != null && !password.isEmpty()) {
            this.pool = new JedisPool(config, host, port, 2000, password);
        } else {
            this.pool = new JedisPool(config, host, port, 2000);
        }
        LOGGER.log(Level.INFO, "Redis cache initialized at {0}:{1}", new Object[]{host, port});
    }

    public void close() {
        pool.close();
    }

    @Override
    public boolean isAvailable() {
        try (Jedis jedis = pool.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    // --- Player Data Cache ---

    public Optional<PlayerData> getCachedPlayer(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            String json = jedis.get(PLAYER_PREFIX + uuid);
            if (json == null) return Optional.empty();
            return Optional.of(deserializePlayer(json));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis GET player failed", e);
            return Optional.empty();
        }
    }

    public void cachePlayer(PlayerData data) {
        try (Jedis jedis = pool.getResource()) {
            String key = PLAYER_PREFIX + data.uuid();
            jedis.setex(key, PLAYER_CACHE_TTL_SECONDS, serializePlayer(data));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis SET player failed", e);
        }
    }

    public void invalidatePlayer(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(PLAYER_PREFIX + uuid);
            jedis.del(PERMS_PREFIX + uuid);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis DEL player failed", e);
        }
    }

    // --- Rank Cache ---

    public Optional<Rank> getCachedRank(String name) {
        try (Jedis jedis = pool.getResource()) {
            String json = jedis.get(RANK_PREFIX + name);
            if (json == null) return Optional.empty();
            return Optional.of(deserializeRank(json));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis GET rank failed", e);
            return Optional.empty();
        }
    }

    public void cacheRank(Rank rank) {
        try (Jedis jedis = pool.getResource()) {
            String key = RANK_PREFIX + rank.name();
            jedis.setex(key, RANK_CACHE_TTL_SECONDS, serializeRank(rank));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis SET rank failed", e);
        }
    }

    public void invalidateRank(String name) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(RANK_PREFIX + name);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis DEL rank failed", e);
        }
    }

    // --- Resolved Permissions Cache (rank perms + extra - denied) ---

    public Optional<Set<String>> getCachedPermissions(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            Set<String> perms = jedis.smembers(PERMS_PREFIX + uuid);
            return perms.isEmpty() ? Optional.empty() : Optional.of(perms);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis SMEMBERS perms failed", e);
            return Optional.empty();
        }
    }

    public void cachePermissions(UUID uuid, Set<String> permissions) {
        try (Jedis jedis = pool.getResource()) {
            String key = PERMS_PREFIX + uuid;
            jedis.del(key);
            if (!permissions.isEmpty()) {
                jedis.sadd(key, permissions.toArray(new String[0]));
                jedis.expire(key, PERMS_CACHE_TTL_SECONDS);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis SADD perms failed", e);
        }
    }

    public void invalidatePermissions(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(PERMS_PREFIX + uuid);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis DEL perms failed", e);
        }
    }

    // --- Invalidation for rank changes (invalidate all players with that rank) ---

    public void invalidateAllPlayersForRank(String rankName) {
        // Scan pattern and invalidate - in production you'd use Redis SET for tracking
        // For now we invalidate the perms cache broadly
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys(PERMS_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    jedis.del(key);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redis SCAN invalidation failed", e);
        }
    }

    // --- Simple JSON Serialization (no external deps) ---

    private String serializePlayer(PlayerData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"uuid\":\"").append(data.uuid()).append("\",");
        sb.append("\"username\":\"").append(escapeJson(data.username())).append("\",");
        sb.append("\"rank\":\"").append(escapeJson(data.rankName())).append("\",");
        sb.append("\"extra\":[");
        appendStringSet(sb, data.extraPermissions());
        sb.append("],");
        sb.append("\"denied\":[");
        appendStringSet(sb, data.deniedPermissions());
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private PlayerData deserializePlayer(String json) {
        String uuid = extractField(json, "uuid");
        String username = extractField(json, "username");
        String rank = extractField(json, "rank");
        Set<String> extra = extractStringArray(json, "extra");
        Set<String> denied = extractStringArray(json, "denied");
        return new PlayerData(UUID.fromString(uuid), username, rank, extra, denied);
    }

    private String serializeRank(Rank rank) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(rank.name())).append("\",");
        sb.append("\"weight\":").append(rank.weight()).append(",");
        sb.append("\"prefix\":\"").append(escapeJson(rank.prefix())).append("\",");
        sb.append("\"color\":\"").append(escapeJson(rank.color())).append("\",");
        sb.append("\"permissions\":[");
        appendStringSet(sb, rank.permissions());
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private Rank deserializeRank(String json) {
        String name = extractField(json, "name");
        int weight = extractInt(json, "weight");
        String prefix = extractField(json, "prefix");
        String color = extractField(json, "color");
        Set<String> perms = extractStringArray(json, "permissions");
        return new Rank(name, weight, prefix, color, perms);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void appendStringSet(StringBuilder sb, Set<String> set) {
        boolean first = true;
        for (String s : set) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(s)).append("\"");
            first = false;
        }
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractInt(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Integer.parseInt(json.substring(start, end));
    }

    private Set<String> extractStringArray(String json, String field) {
        String pattern = "\"" + field + "\":[";
        int start = json.indexOf(pattern);
        if (start == -1) return Set.of();
        start += pattern.length();
        int end = json.indexOf("]", start);
        String content = json.substring(start, end).trim();
        if (content.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String s : content.split(",")) {
            s = s.trim();
            if (s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length() - 1);
            }
            result.add(s);
        }
        return result;
    }
}
