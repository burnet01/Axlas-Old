package rip.wiped.permissions.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.PunishmentData;
import rip.wiped.permissions.model.Rank;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MongoStorage {

    private static final Logger LOGGER = Logger.getLogger("AxlasPermissions");
    private static final String PLAYERS_COLLECTION = "players";
    private static final String RANKS_COLLECTION = "ranks";
    private static final String PUNISHMENTS_COLLECTION = "punishments";

    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> players;
    private final MongoCollection<Document> ranks;
    private final MongoCollection<Document> punishments;

    public MongoStorage(String connectionString, String databaseName) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(databaseName);
        this.players = database.getCollection(PLAYERS_COLLECTION);
        this.ranks = database.getCollection(RANKS_COLLECTION);
        this.punishments = database.getCollection(PUNISHMENTS_COLLECTION);
        LOGGER.log(Level.INFO, "MongoDB connected to database: {0}", databaseName);
    }

    public void close() {
        client.close();
    }

    // --- Player Operations ---

    public Optional<PlayerData> loadPlayer(UUID uuid) {
        Document doc = players.find(Filters.eq("_id", uuid.toString())).first();
        if (doc == null) return Optional.empty();
        return Optional.of(documentToPlayer(doc));
    }

    public Optional<PlayerData> loadPlayerByName(String username) {
        Document doc = players.find(Filters.eq("username", username.toLowerCase())).first();
        if (doc == null) return Optional.empty();
        return Optional.of(documentToPlayer(doc));
    }

    public void savePlayer(PlayerData data) {
        Document doc = new Document("_id", data.uuid().toString())
                .append("username", data.username().toLowerCase())
                .append("rank", data.rankName())
                .append("extraPermissions", new ArrayList<>(data.extraPermissions()))
                .append("deniedPermissions", new ArrayList<>(data.deniedPermissions()));
        players.replaceOne(
                Filters.eq("_id", data.uuid().toString()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    public void createDefaultPlayer(UUID uuid, String username, String defaultRank) {
        PlayerData data = new PlayerData(uuid, username, defaultRank, Set.of(), Set.of());
        savePlayer(data);
    }

    // --- Rank Operations ---

    public Optional<Rank> loadRank(String name) {
        Document doc = ranks.find(Filters.eq("_id", name)).first();
        if (doc == null) return Optional.empty();
        return Optional.of(documentToRank(doc));
    }

    public Map<String, Rank> loadAllRanks() {
        Map<String, Rank> ranks = new ConcurrentHashMap<>();
        for (Document doc : this.ranks.find()) {
            Rank rank = documentToRank(doc);
            ranks.put(rank.name(), rank);
        }
        return ranks;
    }

    public void saveRank(Rank rank) {
        Document doc = new Document("_id", rank.name())
                .append("weight", rank.weight())
                .append("prefix", rank.prefix())
                .append("color", rank.color())
                .append("permissions", new ArrayList<>(rank.permissions()));
        ranks.replaceOne(
                Filters.eq("_id", rank.name()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    public void deleteRank(String name) {
        ranks.deleteOne(Filters.eq("_id", name));
    }

    public boolean rankExists(String name) {
        return ranks.countDocuments(Filters.eq("_id", name)) > 0;
    }

    // --- Punishment Operations ---

    public Map<String, PunishmentData> loadAllPunishments() {
        Map<String, PunishmentData> loaded = new ConcurrentHashMap<>();
        for (Document doc : this.punishments.find()) {
            String key = doc.getString("_id");
            if (key != null) loaded.put(key, documentToPunishment(doc));
        }
        return loaded;
    }

    public void savePunishment(String name, PunishmentData data) {
        Document doc = new Document("_id", name.toLowerCase())
                .append("banExpiresAt", data.banExpiresAt())
                .append("banReason", data.banReason() == null ? "" : data.banReason())
                .append("muteExpiresAt", data.muteExpiresAt())
                .append("muteReason", data.muteReason() == null ? "" : data.muteReason())
                .append("warnings", data.warnings());
        punishments.replaceOne(
                Filters.eq("_id", name.toLowerCase()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    // --- Conversion Helpers ---

    private PlayerData documentToPlayer(Document doc) {
        UUID uuid = UUID.fromString(doc.getString("_id"));
        String username = doc.getString("username");
        String rank = doc.getString("rank");
        List<String> extra = doc.getList("extraPermissions", String.class);
        List<String> denied = doc.getList("deniedPermissions", String.class);
        return new PlayerData(
                uuid,
                username,
                rank,
                extra != null ? new HashSet<>(extra) : Set.of(),
                denied != null ? new HashSet<>(denied) : Set.of()
        );
    }

    private Rank documentToRank(Document doc) {
        String name = doc.getString("_id");
        int weight = doc.getInteger("weight", 0);
        String prefix = doc.getString("prefix");
        String color = doc.getString("color");
        List<String> perms = doc.getList("permissions", String.class);
        return new Rank(name, weight, prefix != null ? prefix : "", color != null ? color : "", perms != null ? new HashSet<>(perms) : Set.of());
    }

    private PunishmentData documentToPunishment(Document doc) {
        return new PunishmentData(
                longValue(doc, "banExpiresAt"),
                nullableString(doc, "banReason"),
                longValue(doc, "muteExpiresAt"),
                nullableString(doc, "muteReason"),
                intValue(doc, "warnings")
        );
    }

    private static long longValue(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static int intValue(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String nullableString(Document doc, String field) {
        String value = doc.getString(field);
        return value == null || value.isEmpty() ? null : value;
    }
}
