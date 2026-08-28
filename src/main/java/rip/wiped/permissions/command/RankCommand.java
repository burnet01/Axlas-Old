package rip.wiped.permissions.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import rip.wiped.permissions.model.Rank;
import rip.wiped.permissions.service.PermissionService;

import java.util.Map;
import java.util.Set;

public final class RankCommand extends Command {

    private final PermissionService service;

    public RankCommand(PermissionService service) {
        super("rank");
        this.service = service;

        setDefaultExecutor((sender, context) -> sendHelp(sender));

        // /rank list
        var list = new Command("list");
        list.setDefaultExecutor((sender, context) -> handleList(sender));
        addSubcommand(list);

        // /rank info <rank>
        var info = new Command("info");
        var infoRank = ArgumentType.String("rank");
        info.addSyntax((sender, context) -> handleInfo(sender, context.get(infoRank)), infoRank);
        addSubcommand(info);

        // /rank create <name> <weight> <prefix>
        var create = new Command("create");
        var createName = ArgumentType.String("name");
        var createWeight = ArgumentType.String("weight");
        var createPrefix = ArgumentType.String("prefix");
        create.addSyntax((sender, context) ->
                handleCreate(sender, context.get(createName), context.get(createWeight), context.get(createPrefix)),
                createName, createWeight, createPrefix);
        addSubcommand(create);

        // /rank delete <rank>
        var delete = new Command("delete");
        var deleteRank = ArgumentType.String("rank");
        delete.addSyntax((sender, context) -> handleDelete(sender, context.get(deleteRank)), deleteRank);
        addSubcommand(delete);

        // /rank addperm <rank> <permission>
        var addperm = new Command("addperm");
        var addpermRank = ArgumentType.String("rank");
        var addpermPerm = ArgumentType.String("permission");
        addperm.addSyntax((sender, context) ->
                handleAddPerm(sender, context.get(addpermRank), context.get(addpermPerm)),
                addpermRank, addpermPerm);
        addSubcommand(addperm);

        // /rank removeperm <rank> <permission>
        var removeperm = new Command("removeperm");
        var removepermRank = ArgumentType.String("rank");
        var removepermPerm = ArgumentType.String("permission");
        removeperm.addSyntax((sender, context) ->
                handleRemovePerm(sender, context.get(removepermRank), context.get(removepermPerm)),
                removepermRank, removepermPerm);
        addSubcommand(removeperm);

        // /rank setweight <rank> <weight>
        var setweight = new Command("setweight");
        var setweightRank = ArgumentType.String("rank");
        var setweightWeight = ArgumentType.String("weight");
        setweight.addSyntax((sender, context) ->
                handleSetWeight(sender, context.get(setweightRank), context.get(setweightWeight)),
                setweightRank, setweightWeight);
        addSubcommand(setweight);

        // /rank setprefix <rank> <prefix>
        var setprefix = new Command("setprefix");
        var setprefixRank = ArgumentType.String("rank");
        var setprefixValue = ArgumentType.String("prefix");
        setprefix.addSyntax((sender, context) ->
                handleSetPrefix(sender, context.get(setprefixRank), context.get(setprefixValue)),
                setprefixRank, setprefixValue);
        addSubcommand(setprefix);

        // /rank setcolor <rank> <color>
        var setcolor = new Command("setcolor");
        var setcolorRank = ArgumentType.String("rank");
        var setcolorValue = ArgumentType.String("color");
        setcolor.addSyntax((sender, context) ->
                handleSetColor(sender, context.get(setcolorRank), context.get(setcolorValue)),
                setcolorRank, setcolorValue);
        addSubcommand(setcolor);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§6§l  Axlas §7Rank Commands");
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank list");
        sender.sendMessage("§7  View all registered ranks");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank info <rank>");
        sender.sendMessage("§7  View detailed information about a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank create <name> <weight> <prefix>");
        sender.sendMessage("§7  Create a new rank with a display prefix");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank delete <rank>");
        sender.sendMessage("§7  Permanently delete a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank addperm <rank> <permission>");
        sender.sendMessage("§7  Grant a permission node to a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank removeperm <rank> <permission>");
        sender.sendMessage("§7  Revoke a permission node from a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank setweight <rank> <weight>");
        sender.sendMessage("§7  Set the priority weight of a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank setprefix <rank> <prefix>");
        sender.sendMessage("§7  Set the display prefix for a rank");
        sender.sendMessage("");
        sender.sendMessage("§e  /rank setcolor <rank> <color>");
        sender.sendMessage("§7  Set the name color for a rank (e.g. &c, &a, &6)");
        sender.sendMessage("§8§m                              ");
    }

    private void handleList(CommandSender sender) {
        Map<String, Rank> ranks = service.getAllRanks();
        if (ranks.isEmpty()) {
            sender.sendMessage("§7No ranks registered.");
            return;
        }
        sender.sendMessage("§8§m                              ");
        sender.sendMessage("§6§l  Registered Ranks §7(" + ranks.size() + ")");
        sender.sendMessage("§8§m                              ");
        ranks.values().stream()
                .sorted((a, b) -> Integer.compare(b.weight(), a.weight()))
                .forEach(r -> sender.sendMessage("  §f" + r.name() + " §8- §7wt: §e" + r.weight() + " §8| §7perms: §e" + r.permissions().size()));
        sender.sendMessage("§8§m                              ");
    }

    private void handleInfo(CommandSender sender, String rankName) {
        service.getRank(rankName).ifPresentOrElse(
                rank -> {
                    sender.sendMessage("§8§m                              ");
                    sender.sendMessage("§6§l  Rank: " + rank.name());
                    sender.sendMessage("§8§m                              ");
                    sender.sendMessage("  §7Weight:    §f" + rank.weight());
                    sender.sendMessage("  §7Prefix:    §f" + rank.prefix());
                    sender.sendMessage("  §7Color:     §f" + rank.color());
                    sender.sendMessage("  §7Perms:     §e" + rank.permissions().size());
                    if (!rank.permissions().isEmpty()) {
                        sender.sendMessage("  §8- §7" + String.join(" §8- §7", rank.permissions()));
                    }
                    sender.sendMessage("§8§m                              ");
                },
                () -> sender.sendMessage("§cRank not found: §f" + rankName)
        );
    }

    private void handleCreate(CommandSender sender, String name, String weightStr, String rankPrefix) {
        if (service.getRank(name).isPresent()) {
            sender.sendMessage("§cRank already exists: §f" + name);
            return;
        }
        int weight;
        try {
            weight = Integer.parseInt(weightStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cWeight must be a valid number.");
            return;
        }
        Rank rank = new Rank(name, weight, rankPrefix, "&f", Set.of());
        service.createRank(rank);
        sender.sendMessage("§aCreated rank §f" + name + " §7with weight §e" + weight);
    }

    private void handleDelete(CommandSender sender, String name) {
        if (name.equals(service.defaultRankName())) {
            sender.sendMessage("§cYou cannot delete the default rank.");
            return;
        }
        if (service.getRank(name).isEmpty()) {
            sender.sendMessage("§cRank not found: §f" + name);
            return;
        }
        service.deleteRank(name);
        sender.sendMessage("§aDeleted rank §f" + name);
    }

    private void handleAddPerm(CommandSender sender, String rankName, String perm) {
        if (service.getRank(rankName).isEmpty()) {
            sender.sendMessage("§cRank not found: §f" + rankName);
            return;
        }
        service.addRankPermission(rankName, perm);
        sender.sendMessage("§aAdded §f" + perm + " §7to rank §f" + rankName);
    }

    private void handleRemovePerm(CommandSender sender, String rankName, String perm) {
        if (service.getRank(rankName).isEmpty()) {
            sender.sendMessage("§cRank not found: §f" + rankName);
            return;
        }
        service.removeRankPermission(rankName, perm);
        sender.sendMessage("§aRemoved §f" + perm + " §7from rank §f" + rankName);
    }

    private void handleSetWeight(CommandSender sender, String rankName, String weightStr) {
        int weight;
        try {
            weight = Integer.parseInt(weightStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cWeight must be a valid number.");
            return;
        }
        service.getRank(rankName).ifPresentOrElse(
                rank -> {
                    Rank updated = new Rank(rank.name(), weight, rank.prefix(), rank.color(), rank.permissions());
                    service.updateRank(updated);
                    sender.sendMessage("§aSet weight of §f" + rankName + " §7to §e" + weight);
                },
                () -> sender.sendMessage("§cRank not found: §f" + rankName)
        );
    }

    private void handleSetPrefix(CommandSender sender, String rankName, String prefix) {
        service.getRank(rankName).ifPresentOrElse(
                rank -> {
                    Rank updated = new Rank(rank.name(), rank.weight(), prefix, rank.color(), rank.permissions());
                    service.updateRank(updated);
                    sender.sendMessage("§aSet prefix of §f" + rankName + " §7to §f" + prefix);
                },
                () -> sender.sendMessage("§cRank not found: §f" + rankName)
        );
    }

    private void handleSetColor(CommandSender sender, String rankName, String color) {
        service.getRank(rankName).ifPresentOrElse(
                rank -> {
                    Rank updated = new Rank(rank.name(), rank.weight(), rank.prefix(), color, rank.permissions());
                    service.updateRank(updated);
                    sender.sendMessage("§aSet color of §f" + rankName + " §7to §f" + color);
                },
                () -> sender.sendMessage("§cRank not found: §f" + rankName)
        );
    }
}
