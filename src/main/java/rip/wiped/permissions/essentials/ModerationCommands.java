package rip.wiped.permissions.essentials;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

import java.util.UUID;

public final class ModerationCommands {

    private static final String PERM_VANISH = "essentials.vanish";
    private static final String PERM_STAFFMODE = "essentials.staffmode";

    private ModerationCommands() {}

    public static void register(Essentials essentials) {
        Command vanish = new Command("vanish", "v");
        vanish.setDefaultExecutor((sender, context) -> handleVanish(essentials, sender));
        Essentials.registerCommand(vanish);

        Command staffmode = new Command("staffmode");
        staffmode.setDefaultExecutor((sender, context) -> handleStaffMode(essentials, sender));
        Essentials.registerCommand(staffmode);
    }

    private static void handleVanish(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_VANISH)) return;
        UUID uuid = player.getUuid();
        boolean vanishing = !essentials.vanished().contains(uuid);
        if (vanishing) {
            essentials.vanished().add(uuid);
            player.setInvisible(true);
        } else {
            essentials.vanished().remove(uuid);
            player.setInvisible(false);
        }
        sender.sendMessage(vanishing ? "§aYou are now vanished." : "§7You are no longer vanished.");
    }

    private static void handleStaffMode(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_STAFFMODE)) return;
        UUID uuid = player.getUuid();
        if (essentials.staffMode().contains(uuid)) {
            essentials.staffMode().remove(uuid);
            essentials.vanished().remove(uuid);
            player.setInvulnerable(false);
            player.setFlying(false);
            player.setInvisible(false);
            sender.sendMessage("§cStaff mode disabled.");
        } else {
            essentials.staffMode().add(uuid);
            essentials.vanished().add(uuid);
            player.setInvulnerable(true);
            player.setAllowFlying(true);
            player.setFlying(true);
            player.setInvisible(true);
            sender.sendMessage("§aStaff mode enabled.");
        }
    }
}