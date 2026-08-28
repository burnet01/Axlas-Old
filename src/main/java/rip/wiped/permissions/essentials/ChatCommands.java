package rip.wiped.permissions.essentials;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.UUID;

public final class ChatCommands {

    private static final String PERM_STAFFCHAT = "essentials.staffchat";
    private static final String PERM_BROADCAST = "essentials.broadcast";
    private static final String PERM_SAY = "essentials.say";

    private ChatCommands() {}

    public static void register(Essentials essentials) {
        registerMsg(essentials);
        registerReply(essentials);
        registerStaffChat(essentials);
        registerBroadcast(essentials);
        registerSay(essentials);
    }

    private static void registerMsg(Essentials essentials) {
        var msg = new Command("msg", "tell", "w");
        var target = playerArg("target");
        var message = ArgumentType.StringArray("message");
        msg.addSyntax((sender, context) ->
                handleMsg(essentials, sender, context.get(target), String.join(" ", context.get(message))),
                target, message);
        Essentials.registerCommand(msg);
    }

    private static void handleMsg(Essentials essentials, CommandSender sender, String targetName, String message) {
        Player senderPlayer = Essentials.asPlayer(sender);
        if (senderPlayer == null) return;
        Player target = Essentials.findOnline(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: §f" + targetName);
            return;
        }
        sender.sendMessage("§7[§fYou §7-> §f" + target.getUsername() + "§7] §f" + message);
        target.sendMessage("§7[§f" + senderPlayer.getUsername() + " §7-> §fYou§7] §f" + message);
        essentials.replyTargets().put(senderPlayer.getUuid(), target.getUuid());
    }

    private static void registerReply(Essentials essentials) {
        var reply = new Command("reply", "r");
        var message = ArgumentType.StringArray("message");
        reply.addSyntax((sender, context) ->
                handleReply(essentials, sender, String.join(" ", context.get(message))),
                message);
        Essentials.registerCommand(reply);
    }

    private static void handleReply(Essentials essentials, CommandSender sender, String message) {
        Player senderPlayer = Essentials.asPlayer(sender);
        if (senderPlayer == null) return;
        UUID targetId = essentials.replyTargets().get(senderPlayer.getUuid());
        if (targetId == null) {
            sender.sendMessage("§cYou have no one to reply to.");
            return;
        }
        Player target = findOnline(targetId);
        if (target == null) {
            sender.sendMessage("§cThat player is no longer online.");
            return;
        }
        sender.sendMessage("§7[§fYou §7-> §f" + target.getUsername() + "§7] §f" + message);
        target.sendMessage("§7[§f" + senderPlayer.getUsername() + " §7-> §fYou§7] §f" + message);
    }

    private static void registerStaffChat(Essentials essentials) {
        var staffchat = new Command("staffchat", "sc");
        var message = ArgumentType.StringArray("message");
        staffchat.addSyntax((sender, context) ->
                handleStaffChat(essentials, sender, String.join(" ", context.get(message))),
                message);
        Essentials.registerCommand(staffchat);
    }

    private static void handleStaffChat(Essentials essentials, CommandSender sender, String message) {
        Player senderPlayer = Essentials.asPlayer(sender);
        if (senderPlayer == null || !essentials.requirePermission(sender, PERM_STAFFCHAT)) return;
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (essentials.service().hasPermission(player.getUuid(), PERM_STAFFCHAT)) {
                player.sendMessage("§8[§6Staff§8] §f" + senderPlayer.getUsername() + "§8: §7" + message);
            }
        }
    }

    private static void registerBroadcast(Essentials essentials) {
        var broadcast = new Command("broadcast", "bc");
        var message = ArgumentType.StringArray("message");
        broadcast.addSyntax((sender, context) ->
                handleBroadcast(essentials, sender, String.join(" ", context.get(message))),
                message);
        Essentials.registerCommand(broadcast);
    }

    private static void handleBroadcast(Essentials essentials, CommandSender sender, String message) {
        if (!essentials.requirePermission(sender, PERM_BROADCAST)) return;
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.sendMessage("§6[§eBroadcast§6] §f" + message);
        }
    }

    private static void registerSay(Essentials essentials) {
        var say = new Command("say");
        var message = ArgumentType.StringArray("message");
        say.addSyntax((sender, context) ->
                handleSay(essentials, sender, String.join(" ", context.get(message))),
                message);
        Essentials.registerCommand(say);
    }

    private static void handleSay(Essentials essentials, CommandSender sender, String message) {
        if (!essentials.requirePermission(sender, PERM_SAY)) return;
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.sendMessage("§7[Server] §f" + message);
        }
    }

    private static Player findOnline(UUID uuid) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getUuid().equals(uuid)) return player;
        }
        return null;
    }

    private static Argument<String> playerArg(String id) {
        Argument<String> arg = ArgumentType.String(id);
        arg.setSuggestionCallback((sender, context, suggestion) -> suggestPlayers(suggestion));
        return arg;
    }

    private static void suggestPlayers(Suggestion suggestion) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            suggestion.addEntry(new SuggestionEntry(player.getUsername()));
        }
    }
}