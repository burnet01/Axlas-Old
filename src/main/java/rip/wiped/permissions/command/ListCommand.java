package rip.wiped.permissions.command;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import rip.wiped.permissions.FakePlayerManager;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;
import rip.wiped.permissions.service.PermissionService;

import java.util.*;
import java.util.stream.Collectors;

public final class ListCommand extends Command {

    private final PermissionService service;

    public ListCommand(PermissionService service) {
        super("list", "who", "online");
        this.service = service;

        setDefaultExecutor((sender, context) -> handleList(sender));
    }

    private void handleList(CommandSender sender) {
        Collection<Player> online = MinecraftServer.getConnectionManager().getOnlinePlayers();
        int fakeCount = FakePlayerManager.getCount();
        int maxPlayers = online.size() + fakeCount;

        Collection<Rank> allRanks = service.getAllRanks().values();

        List<Rank> sortedRanks = allRanks.stream()
                .sorted((a, b) -> Integer.compare(b.weight(), a.weight()))
                .collect(Collectors.toList());

        StringBuilder legendBuilder = new StringBuilder();
        for (int i = 0; i < sortedRanks.size(); i++) {
            Rank rank = sortedRanks.get(i);
            String colorCode = rankColorToCode(parseColor(rank.color()));
            legendBuilder.append(colorCode).append(rank.name());
            if (i < sortedRanks.size() - 1) {
                legendBuilder.append("§7, ");
            }
        }
        sender.sendMessage(legendBuilder.toString());

        int totalPlayers = online.size() + fakeCount;
        sender.sendMessage("§f(" + totalPlayers + "§f/" + maxPlayers + "):");

        if (online.isEmpty() && fakeCount == 0) {
            return;
        }

        List<PlayerEntry> playerEntries = new ArrayList<>();

        for (Player player : online) {
            PlayerData data = service.getPlayerByName(player.getUsername()).orElse(null);
            String rankName = data != null ? data.rankName() : service.defaultRankName();
            Rank rank = service.getRank(rankName).orElse(null);
            int weight = rank != null ? rank.weight() : 0;
            String colorCode = rank != null ? rankColorToCode(parseColor(rank.color())) : "§f";
            playerEntries.add(new PlayerEntry(player.getUsername(), colorCode, weight));
        }

        for (Map.Entry<String, String> fake : FakePlayerManager.getAll().entrySet()) {
            String rankName = fake.getValue();
            Rank rank = service.getRank(rankName).orElse(null);
            int weight = rank != null ? rank.weight() : 0;
            String colorCode = rank != null ? rankColorToCode(parseColor(rank.color())) : "§f";
            playerEntries.add(new PlayerEntry(fake.getKey(), colorCode, weight));
        }

        playerEntries.sort((a, b) -> {
            int weightComp = Integer.compare(b.weight(), a.weight());
            if (weightComp != 0) return weightComp;
            return a.name().compareToIgnoreCase(b.name());
        });

        StringBuilder playersLine = new StringBuilder();
        for (int i = 0; i < playerEntries.size(); i++) {
            PlayerEntry entry = playerEntries.get(i);
            playersLine.append(entry.colorCode()).append(entry.name());
            if (i < playerEntries.size() - 1) {
                playersLine.append("§f, ");
            }
        }
        sender.sendMessage(playersLine.toString());
    }

    private TextColor parseColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return NamedTextColor.WHITE;
        for (int i = 0; i < colorCode.length(); i++) {
            char c = colorCode.charAt(i);
            if ("0123456789abcdefABCDEF".indexOf(c) != -1) {
                return switch (Character.toLowerCase(c)) {
                    case '0' -> NamedTextColor.BLACK;
                    case '1' -> NamedTextColor.DARK_BLUE;
                    case '2' -> NamedTextColor.DARK_GREEN;
                    case '3' -> NamedTextColor.DARK_AQUA;
                    case '4' -> NamedTextColor.DARK_RED;
                    case '5' -> NamedTextColor.DARK_PURPLE;
                    case '6' -> NamedTextColor.GOLD;
                    case '7' -> NamedTextColor.GRAY;
                    case '8' -> NamedTextColor.DARK_GRAY;
                    case '9' -> NamedTextColor.BLUE;
                    case 'a' -> NamedTextColor.GREEN;
                    case 'b' -> NamedTextColor.AQUA;
                    case 'c' -> NamedTextColor.RED;
                    case 'd' -> NamedTextColor.LIGHT_PURPLE;
                    case 'e' -> NamedTextColor.YELLOW;
                    default -> NamedTextColor.WHITE;
                };
            }
        }
        return NamedTextColor.WHITE;
    }

    private String rankColorToCode(TextColor color) {
        if (color == null) return "§f";
        if (color.equals(NamedTextColor.BLACK)) return "§0";
        if (color.equals(NamedTextColor.DARK_BLUE)) return "§1";
        if (color.equals(NamedTextColor.DARK_GREEN)) return "§2";
        if (color.equals(NamedTextColor.DARK_AQUA)) return "§3";
        if (color.equals(NamedTextColor.DARK_RED)) return "§4";
        if (color.equals(NamedTextColor.DARK_PURPLE)) return "§5";
        if (color.equals(NamedTextColor.GOLD)) return "§6";
        if (color.equals(NamedTextColor.GRAY)) return "§7";
        if (color.equals(NamedTextColor.DARK_GRAY)) return "§8";
        if (color.equals(NamedTextColor.BLUE)) return "§9";
        if (color.equals(NamedTextColor.GREEN)) return "§a";
        if (color.equals(NamedTextColor.AQUA)) return "§b";
        if (color.equals(NamedTextColor.RED)) return "§c";
        if (color.equals(NamedTextColor.LIGHT_PURPLE)) return "§d";
        if (color.equals(NamedTextColor.YELLOW)) return "§e";
        return "§f";
    }

    private record PlayerEntry(String name, String colorCode, int weight) {}
}
