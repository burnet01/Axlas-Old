package rip.wiped.permissions.essentials;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.StringJoiner;

public final class PunishmentCommands {

    private static final String PERM_KICK = "essentials.kick";
    private static final String PERM_BAN = "essentials.ban";
    private static final String PERM_UNBAN = "essentials.unban";
    private static final String PERM_MUTE = "essentials.mute";
    private static final String PERM_UNMUTE = "essentials.unmute";
    private static final String PERM_WARN = "essentials.warn";

    private PunishmentCommands() {}

    public static void register(Essentials essentials) {
        registerKick(essentials);
        registerBan(essentials);
        registerUnban(essentials);
        registerMute(essentials);
        registerUnmute(essentials);
        registerWarn(essentials);
    }

    private static void registerKick(Essentials essentials) {
        var kick = new Command("kick");
        var target = ArgumentType.String("target");
        target.setSuggestionCallback((sender, context, suggestion) -> suggestPlayers(suggestion));
        var reason = ArgumentType.StringArray("reason");
        kick.addSyntax((sender, context) ->
                handleKick(essentials, sender, context.get(target), join(context, reason)),
                target, reason);
        kick.addSyntax((sender, context) ->
                handleKick(essentials, sender, context.get(target), "You have been kicked."),
                target);
        Essentials.registerCommand(kick);
    }

    private static void handleKick(Essentials essentials, CommandSender sender, String targetName, String reason) {
        if (!essentials.requirePermission(sender, PERM_KICK)) return;
        Player target = Essentials.findOnline(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        target.kick(reason);
        sender.sendMessage("§aKicked §f" + target.getUsername() + "§a.");
    }

    private static void registerBan(Essentials essentials) {
        var ban = new Command("ban");
        var target = ArgumentType.String("target");
        var reason = ArgumentType.StringArray("reason");
        ban.addSyntax((sender, context) ->
                handleBan(essentials, sender, context.get(target), join(context, reason)),
                target, reason);
        ban.addSyntax((sender, context) ->
                handleBan(essentials, sender, context.get(target), "You have been banned."),
                target);
        Essentials.registerCommand(ban);
    }

    private static void handleBan(Essentials essentials, CommandSender sender, String targetName, String reason) {
        if (!essentials.requirePermission(sender, PERM_BAN)) return;
        boolean added = essentials.punishments().ban(targetName, reason, Long.MAX_VALUE);
        if (!added) {
            sender.sendMessage("§c" + targetName + " is already banned.");
            return;
        }
        Player target = Essentials.findOnline(targetName);
        if (target != null) target.kick("§cYou have been banned.\n" + reason);
        sender.sendMessage("§aBanned §f" + targetName + "§a.");
    }

    private static void registerUnban(Essentials essentials) {
        var unban = new Command("unban");
        var target = ArgumentType.String("target");
        unban.addSyntax((sender, context) -> handleUnban(essentials, sender, context.get(target)), target);
        Essentials.registerCommand(unban);
    }

    private static void handleUnban(Essentials essentials, CommandSender sender, String targetName) {
        if (!essentials.requirePermission(sender, PERM_UNBAN)) return;
        if (!essentials.punishments().unban(targetName)) {
            sender.sendMessage("§c" + targetName + " isn't banned.");
            return;
        }
        sender.sendMessage("§aUnbanned §f" + targetName + "§a.");
    }

    private static void registerMute(Essentials essentials) {
        var mute = new Command("mute");
        var target = ArgumentType.String("target");
        target.setSuggestionCallback((sender, context, suggestion) -> suggestPlayers(suggestion));
        var reason = ArgumentType.StringArray("reason");
        mute.addSyntax((sender, context) ->
                handleMute(essentials, sender, context.get(target), join(context, reason)),
                target, reason);
        mute.addSyntax((sender, context) ->
                handleMute(essentials, sender, context.get(target), "You have been muted."),
                target);
        Essentials.registerCommand(mute);
    }

    private static void handleMute(Essentials essentials, CommandSender sender, String targetName, String reason) {
        if (!essentials.requirePermission(sender, PERM_MUTE)) return;
        boolean added = essentials.punishments().mute(targetName, reason, Long.MAX_VALUE);
        if (!added) {
            sender.sendMessage("§c" + targetName + " is already muted.");
            return;
        }
        sender.sendMessage("§aMuted §f" + targetName + "§a.");
    }

    private static void registerUnmute(Essentials essentials) {
        var unmute = new Command("unmute");
        var target = ArgumentType.String("target");
        unmute.addSyntax((sender, context) -> handleUnmute(essentials, sender, context.get(target)), target);
        Essentials.registerCommand(unmute);
    }

    private static void handleUnmute(Essentials essentials, CommandSender sender, String targetName) {
        if (!essentials.requirePermission(sender, PERM_UNMUTE)) return;
        if (!essentials.punishments().unmute(targetName)) {
            sender.sendMessage("§c" + targetName + " isn't muted.");
            return;
        }
        sender.sendMessage("§aUnmuted §f" + targetName + "§a.");
    }

    private static void registerWarn(Essentials essentials) {
        var warn = new Command("warn");
        var target = ArgumentType.String("target");
        target.setSuggestionCallback((sender, context, suggestion) -> suggestPlayers(suggestion));
        var reason = ArgumentType.StringArray("reason");
        warn.addSyntax((sender, context) ->
                handleWarn(essentials, sender, context.get(target), join(context, reason)),
                target, reason);
        Essentials.registerCommand(warn);
    }

    private static void handleWarn(Essentials essentials, CommandSender sender, String targetName, String reason) {
        if (!essentials.requirePermission(sender, PERM_WARN)) return;
        essentials.punishments().warn(targetName);
        int warnings = essentials.punishments().warningCount(targetName);
        Player target = Essentials.findOnline(targetName);
        if (target != null) {
            target.sendMessage("§cYou have been warned. (" + warnings + ") §8- §7" + reason);
        }
        sender.sendMessage("§aWarned §f" + targetName + "§a. (Total: " + warnings + ")");
    }

    private static String join(CommandContext context, Argument<String[]> argument) {
        String[] parts = context.get(argument);
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : parts) joiner.add(part);
        return joiner.toString();
    }

    private static void suggestPlayers(Suggestion suggestion) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            suggestion.addEntry(new SuggestionEntry(player.getUsername()));
        }
    }
}