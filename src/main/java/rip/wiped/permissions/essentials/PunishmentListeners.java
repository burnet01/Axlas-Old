package rip.wiped.permissions.essentials;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;

/** Enforces punishments at login and while chatting. */
public final class PunishmentListeners {

    private PunishmentListeners() {}

    public static void register(Essentials essentials) {
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class,
                event -> enforceBan(essentials, event));

        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent.class,
                event -> enforceMute(essentials, event));
    }

    private static void enforceBan(Essentials essentials, AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        String name = player.getUsername();
        if (!essentials.punishments().isBanned(name)) return;
        String reason = essentials.punishments().banReason(name);
        player.kick(Component.text(reason != null ? "§cYou are banned.\n" + reason : "§cYou are banned."));
    }

    private static void enforceMute(Essentials essentials, PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!essentials.punishments().isMuted(player.getUsername())) return;
        event.setCancelled(true);
        player.sendMessage("§cYou are muted and cannot chat.");
    }
}