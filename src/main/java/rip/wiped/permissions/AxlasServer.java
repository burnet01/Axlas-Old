package rip.wiped.permissions;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.ping.Status;
import net.minestom.server.utils.identity.NamedAndIdentified;
import rip.wiped.permissions.essentials.Essentials;
import rip.wiped.permissions.service.PermissionService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AxlasServer {

    private static final Logger LOGGER = Logger.getLogger("AxlasServer");

    private AxlasPermissions permissions;

    public void start(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        permissions = AxlasPermissions.bootstrap();
        Essentials.register(permissions.getService());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        // Set the ChunkGenerator
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        // Add an event callback to specify the spawning instance (and the spawn position)
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
        });

        LOGGER.log(Level.INFO, "AxlasServer starting...");
        minecraftServer.start("0.0.0.0", 25565);
        LOGGER.log(Level.INFO, "AxlasServer started on port 25565.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.log(Level.INFO, "Shutting down...");
            if (permissions != null) permissions.shutdown();
        }));
    }

    public PermissionService getPermissionService() {
        return permissions != null ? permissions.getService() : null;
    }

    public AxlasPermissions getPermissions() {
        return permissions;
    }

    public static void main(String[] args) {
        new AxlasServer().start(args);
    }
}
