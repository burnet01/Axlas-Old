package rip.wiped.permissions.essentials;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public final class TeleportCommands {

    private static final String PERM_SPAWN = "essentials.spawn";
    private static final String PERM_SETSPAWN = "essentials.setspawn";
    private static final String PERM_FLY = "essentials.fly";
    private static final String PERM_SPEED = "essentials.speed";

    private TeleportCommands() {}

    public static void register(Essentials essentials) {
        Command spawn = new Command("spawn");
        spawn.setDefaultExecutor((sender, context) -> handleSpawn(essentials, sender));
        Essentials.registerCommand(spawn);

        Command setspawn = new Command("setspawn");
        setspawn.setDefaultExecutor((sender, context) -> handleSetSpawn(essentials, sender));
        Essentials.registerCommand(setspawn);

        Command fly = new Command("fly");
        fly.setDefaultExecutor((sender, context) -> handleFly(essentials, sender));
        Essentials.registerCommand(fly);

        var speed = new Command("speed");
        var speedAmount = ArgumentType.Integer("amount");
        speed.addSyntax((sender, context) -> handleSpeed(essentials, sender, context.get(speedAmount)), speedAmount);
        Essentials.registerCommand(speed);

        Command ping = new Command("ping");
        ping.setDefaultExecutor((sender, context) -> handlePing(sender));
        Essentials.registerCommand(ping);
    }

    private static void handleSpawn(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_SPAWN)) return;
        Pos spawnPos = essentials.spawnPos();
        if (spawnPos == null) {
            sender.sendMessage("§cSpawn hasn't been set yet.");
            return;
        }
        Instance spawnInstance = essentials.spawnInstance();
        if (spawnInstance != null && !spawnInstance.equals(player.getInstance())) {
            player.setInstance(spawnInstance, spawnPos);
        } else {
            player.teleport(spawnPos);
        }
        sender.sendMessage("§aTeleported to spawn.");
    }

    private static void handleSetSpawn(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_SETSPAWN)) return;
        essentials.setSpawn(player.getPosition(), player.getInstance());
        sender.sendMessage("§aSpawn set to your current location.");
    }

    private static void handleFly(Essentials essentials, CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_FLY)) return;
        boolean flying = !player.isFlying();
        player.setAllowFlying(true);
        player.setFlying(flying);
        sender.sendMessage(flying ? "§aFlying enabled." : "§7Flying disabled.");
    }

    private static void handleSpeed(Essentials essentials, CommandSender sender, float amount) {
        Player player = Essentials.asPlayer(sender);
        if (player == null || !essentials.requirePermission(sender, PERM_SPEED)) return;
        float clamped = Math.max(0f, Math.min(1f, amount / 10f));
        player.setFlyingSpeed(clamped);
        sender.sendMessage("§aFlying speed set to " + amount);
    }

    private static void handlePing(CommandSender sender) {
        Player player = Essentials.asPlayer(sender);
        if (player == null) return;
        sender.sendMessage("§7Your ping: §f" + player.getLatency() + "ms");
    }
}