package rip.wiped.permissions.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;
import rip.wiped.permissions.service.PermissionService;

import java.util.UUID;

public final class PermissionCommand extends Command {

    private final PermissionService service;

    public PermissionCommand(PermissionService service) {
        super("permission", "perm");
        this.service = service;

        setDefaultExecutor((sender, context) -> sendHelp(sender));

        // /perm check <player> <permission>
        var check = new Command("check");
        var checkPlayer = playerArgument("player");
        var checkPerm = ArgumentType.String("permission");
        check.addSyntax((sender, context) ->
                handleCheck(sender, context.get(checkPlayer), context.get(checkPerm)),
                checkPlayer, checkPerm);
        addSubcommand(check);

        // /perm setrank <player> <rank>
        var setrank = new Command("setrank");
        var setrankPlayer = playerArgument("player");
        var setrankRank = ArgumentType.String("rank");
        setrank.addSyntax((sender, context) ->
                handleSetRank(sender, context.get(setrankPlayer), context.get(setrankRank)),
                setrankPlayer, setrankRank);
        addSubcommand(setrank);

        // /perm addperm <player> <permission>
        var addperm = new Command("addperm");
        var addpermPlayer = playerArgument("player");
        var addpermPerm = ArgumentType.String("permission");
        addperm.addSyntax((sender, context) ->
                handleAddPerm(sender, context.get(addpermPlayer), context.get(addpermPerm)),
                addpermPlayer, addpermPerm);
        addSubcommand(addperm);

        // /perm removeperm <player> <permission>
        var removeperm = new Command("removeperm");
        var removepermPlayer = playerArgument("player");
        var removepermPerm = ArgumentType.String("permission");
        removeperm.addSyntax((sender, context) ->
                handleRemovePerm(sender, context.get(removepermPlayer), context.get(removepermPerm)),
                removepermPlayer, removepermPerm);
        addSubcommand(removeperm);

        // /perm deny <player> <permission>
        var deny = new Command("deny");
        var denyPlayer = playerArgument("player");
        var denyPerm = ArgumentType.String("permission");
        deny.addSyntax((sender, context) ->
                handleDeny(sender, context.get(denyPlayer), context.get(denyPerm)),
                denyPlayer, denyPerm);
        addSubcommand(deny);

        // /perm undeny <player> <permission>
        var undeny = new Command("undeny");
        var undenyPlayer = playerArgument("player");
        var undenyPerm = ArgumentType.String("permission");
        undeny.addSyntax((sender, context) ->
                handleUnDeny(sender, context.get(undenyPlayer), context.get(undenyPerm)),
                undenyPlayer, undenyPerm);
        addSubcommand(undeny);

        // /perm info <player>
        var info = new Command("info");
        var infoPlayer = playerArgument("player");
        info.addSyntax((sender, context) -> handleInfo(sender, context.get(infoPlayer)), infoPlayer);
        addSubcommand(info);
    }

    private static Argument<String> playerArgument(String id) {
        Argument<String> arg = ArgumentType.String(id);
        arg.setSuggestionCallback((sender, context, suggestion) -> PlayerSuggestions.suggest(suggestion));
        return arg;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§6§l  Axlas §7Permission Commands");
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm check <player> <permission>");
        sender.sendMessage("§7  Check if a player has a specific permission");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm setrank <player> <rank>");
        sender.sendMessage("§7  Assign a rank to a player");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm addperm <player> <permission>");
        sender.sendMessage("§7  Grant an extra permission to a player");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm removeperm <player> <permission>");
        sender.sendMessage("§7  Revoke an extra permission from a player");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm deny <player> <permission>");
        sender.sendMessage("§7  Deny a permission for a player");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm undeny <player> <permission>");
        sender.sendMessage("§7  Remove a denied permission from a player");
        sender.sendMessage("");
        sender.sendMessage("§e  /perm info <player>");
        sender.sendMessage("§7  View a player's full permission profile");
        sender.sendMessage("§8§m                              ");
    }

    private UUID findPlayerUuid(String name) {
        return service.getPlayerByName(name).map(PlayerData::uuid).orElse(null);
    }

    private void handleCheck(CommandSender sender, String playerName, String perm) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        boolean has = service.hasPermission(targetUuid, perm);
        if (has) {
            sender.sendMessage("§a§f" + playerName + " §7has permission §f" + perm);
        } else {
            sender.sendMessage("§c§f" + playerName + " §7does not have §f" + perm);
        }
    }

    private void handleSetRank(CommandSender sender, String playerName, String rankName) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        if (service.getRank(rankName).isEmpty()) {
            sender.sendMessage("§cRank not found: §f" + rankName);
            return;
        }
        service.setPlayerRank(targetUuid, rankName);
        sender.sendMessage("§aSet §f" + playerName + "'s §7rank to §f" + rankName);
    }

    private void handleAddPerm(CommandSender sender, String playerName, String perm) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        service.addExtraPermission(targetUuid, perm);
        sender.sendMessage("§aAdded §f" + perm + " §7to §f" + playerName);
    }

    private void handleRemovePerm(CommandSender sender, String playerName, String perm) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        service.removeExtraPermission(targetUuid, perm);
        sender.sendMessage("§aRemoved §f" + perm + " §7from §f" + playerName);
    }

    private void handleDeny(CommandSender sender, String playerName, String perm) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        service.denyPermission(targetUuid, perm);
        sender.sendMessage("§aDenied §f" + perm + " §7for §f" + playerName);
    }

    private void handleUnDeny(CommandSender sender, String playerName, String perm) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        service.unDenyPermission(targetUuid, perm);
        sender.sendMessage("§aRemoved deny for §f" + perm + " §7on §f" + playerName);
    }

    private void handleInfo(CommandSender sender, String playerName) {
        UUID targetUuid = findPlayerUuid(playerName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found: §f" + playerName);
            return;
        }
        PlayerData data = service.getOrCreatePlayer(targetUuid, playerName);
        Rank rank = service.getRank(data.rankName()).orElse(null);
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§6§l  Permission Profile: " + playerName);
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("  §7UUID:    §f" + data.uuid());
        sender.sendMessage("  §7Rank:    §f" + data.rankName() + (rank != null ? " §8(§7wt: §e" + rank.weight() + "§8)" : ""));
        sender.sendMessage("  §7Extra:   §f" + (data.extraPermissions().isEmpty() ? "§7none" : data.extraPermissions()));
        sender.sendMessage("  §7Denied:  §f" + (data.deniedPermissions().isEmpty() ? "§7none" : data.deniedPermissions()));
        sender.sendMessage("  §7Total:   §e" + service.resolvePermissions(data).size() + " §7permissions resolved");
        sender.sendMessage("§8§m                              ");
    }
}
