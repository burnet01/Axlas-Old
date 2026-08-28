package rip.wiped.permissions.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.service.PermissionService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerConnectionListener {

    private final PermissionService service;
    private final ConcurrentHashMap<UUID, PlayerData> sessionCache = new ConcurrentHashMap<>();

    public PlayerConnectionListener(PermissionService service) {
        this.service = service;
    }

    public void register(EventNode<Event> node) {
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUuid();
            String username = player.getUsername();
            PlayerData data = service.getOrCreatePlayer(uuid, username);
            sessionCache.put(uuid, data);
        });

        node.addListener(PlayerDisconnectEvent.class, event -> {
            UUID uuid = event.getPlayer().getUuid();
            sessionCache.remove(uuid);
        });
    }

    public PlayerData getSessionData(UUID uuid) {
        return sessionCache.get(uuid);
    }

    public void refreshSession(UUID uuid) {
        PlayerData data = sessionCache.get(uuid);
        if (data != null) {
            service.getOrCreatePlayer(uuid, data.username());
        }
    }

    public void invalidateSession(UUID uuid) {
        sessionCache.remove(uuid);
    }
}
