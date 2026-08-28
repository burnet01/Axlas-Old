package rip.wiped.permissions;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import rip.wiped.permissions.cache.LocalCache;
import rip.wiped.permissions.cache.PermissionCache;
import rip.wiped.permissions.cache.RedisCache;
import rip.wiped.permissions.command.ListCommand;
import rip.wiped.permissions.command.PermissionCommand;
import rip.wiped.permissions.command.RankCommand;
import rip.wiped.permissions.listener.ChatHandler;
import rip.wiped.permissions.listener.PlayerConnectionListener;
import rip.wiped.permissions.service.PermissionService;
import rip.wiped.permissions.storage.MongoStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core permissions system. Owns its own configuration and wires itself into the running
 * Minestom server, so it can be dropped into any other server (or Main) unchanged.
 */
public final class AxlasPermissions {

    private static final Logger LOGGER = Logger.getLogger("AxlasPermissions");
    private static final String DEFAULT_CONFIG_RESOURCE = "axlas.properties";

    private final MongoStorage mongo;
    private final PermissionCache cache;
    private final PermissionService service;
    private final PlayerConnectionListener listener;
    private final ChatHandler chatHandler;

    private AxlasPermissions(String mongoUri, String mongoDatabase,
                             String redisHost, int redisPort, String redisPassword) {
        LOGGER.log(Level.INFO, "Initializing AxlasPermissions...");

        this.mongo = new MongoStorage(mongoUri, mongoDatabase);
        this.cache = createCache(redisHost, redisPort, redisPassword);
        this.service = new PermissionService(mongo, cache);

        this.listener = new PlayerConnectionListener(service);
        listener.register(MinecraftServer.getGlobalEventHandler());

        this.chatHandler = new ChatHandler(service);
        MinecraftServer.getGlobalEventHandler().addChild(chatHandler.createNode());

        CommandManager cmdManager = MinecraftServer.getCommandManager();
        cmdManager.register(new PermissionCommand(service));
        cmdManager.register(new RankCommand(service));
        cmdManager.register(new ListCommand(service));

        LOGGER.log(Level.INFO, "AxlasPermissions initialized.");
    }

    /**
     * Boot the permissions system using the bundled {@code axlas.properties} config.
     */
    public static AxlasPermissions bootstrap() {
        return fromConfig(loadConfig(DEFAULT_CONFIG_RESOURCE));
    }

    /**
     * Boot using a specific config resource (e.g. {@code "my-axlas.properties"}).
     */
    public static AxlasPermissions bootstrap(String configResource) {
        return fromConfig(loadConfig(configResource));
    }

    /**
     * Boot using externally provided configuration, useful when embedding from another
     * server's own config.
     */
    public static AxlasPermissions from(Properties config) {
        return fromConfig(config);
    }

    private static AxlasPermissions fromConfig(Properties config) {
        String mongoUri = config.getProperty("mongo.uri", "mongodb://localhost:27017");
        String mongoDatabase = config.getProperty("mongo.database", "axlas_permissions");
        String redisHost = config.getProperty("redis.host", "localhost");
        int redisPort = Integer.parseInt(config.getProperty("redis.port", "6379"));
        String redisPassword = config.getProperty("redis.password", "");
        return new AxlasPermissions(mongoUri, mongoDatabase, redisHost, redisPort, redisPassword);
    }

    private static Properties loadConfig(String resourceName) {
        Properties config = new Properties();
        try (InputStream is = AxlasPermissions.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                config.load(is);
            } else {
                LOGGER.log(Level.WARNING, resourceName + " not found, using defaults.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + resourceName, e);
        }
        return config;
    }

    private static PermissionCache createCache(String redisHost, int redisPort, String redisPassword) {
        RedisCache redis = new RedisCache(redisHost, redisPort, redisPassword);
        if (redis.isAvailable()) {
            return redis;
        }
        redis.close();
        LOGGER.log(Level.WARNING, "Redis unavailable at " + redisHost + ":" + redisPort
                + " - falling back to local in-memory cache. Cross-server invalidation disabled.");
        return new LocalCache();
    }

    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down AxlasPermissions...");
        if (mongo != null) mongo.close();
        if (cache != null) cache.close();
    }

    public PermissionService getService() {
        return service;
    }

    public PlayerConnectionListener getListener() {
        return listener;
    }

    public ChatHandler getChatHandler() {
        return chatHandler;
    }

    public PermissionCache getCache() {
        return cache;
    }

    public MongoStorage getMongo() {
        return mongo;
    }
}