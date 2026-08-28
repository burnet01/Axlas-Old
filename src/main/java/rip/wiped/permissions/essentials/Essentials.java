package rip.wiped.permissions.essentials;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import rip.wiped.permissions.service.PermissionService;
import rip.wiped.permissions.storage.MongoStorage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds shared state for the essential commands and registers them all.
 * Commands are permission-gated through the permissions system
 * (console bypasses permission checks).
 */
public final class Essentials {

    private final PermissionService service;
    private final PunishmentManager punishments;
    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();

    private Pos spawnPos;
    private Instance spawnInstance;

    private Essentials(PermissionService service, MongoStorage mongo) {
        this.service = service;
        this.punishments = new PunishmentManager(mongo);
    }

    public static Essentials register(PermissionService service) {
        Essentials essentials = new Essentials(service, service.mongoStorage());
        TeleportCommands.register(essentials);
        GameCommands.register(essentials);
        ChatCommands.register(essentials);
        ModerationCommands.register(essentials);
        PunishmentCommands.register(essentials);
        PunishmentListeners.register(essentials);
        return essentials;
    }

    public PermissionService service() {
        return service;
    }

    public PunishmentManager punishments() {
        return punishments;
    }

    public Map<UUID, UUID> replyTargets() {
        return replyTargets;
    }

    public Set<UUID> vanished() {
        return vanished;
    }

    public Set<UUID> staffMode() {
        return staffMode;
    }

    public Pos spawnPos() {
        return spawnPos;
    }

    public Instance spawnInstance() {
        return spawnInstance;
    }

    public void setSpawn(Pos pos, Instance instance) {
        this.spawnPos = pos;
        this.spawnInstance = instance;
    }

    public static void registerCommand(Command command) {
        MinecraftServer.getCommandManager().register(command);
    }

    public static Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage("§cThis command can only be used by players.");
        return null;
    }

    public static Player findOnline(String name) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getUsername().equalsIgnoreCase(name)) return player;
        }
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getUsername().toLowerCase().startsWith(name.toLowerCase())) return player;
        }
        return null;
    }

    public boolean requirePermission(CommandSender sender, String node) {
        if (!(sender instanceof Player player)) return true;
        if (service.hasPermission(player.getUuid(), node)) return true;
        sender.sendMessage("§cYou don't have permission to use this command.");
        return false;
    }
}