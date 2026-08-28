package rip.wiped.permissions.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import rip.wiped.permissions.model.PlayerData;
import rip.wiped.permissions.model.Rank;
import rip.wiped.permissions.service.PermissionService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatHandler {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final TextColor DEFAULT_COLOR = NamedTextColor.WHITE;
    private static final Component EMPTY_PREFIX = Component.empty();

    private final PermissionService service;
    private final Map<String, RankDisplay> displayCache = new ConcurrentHashMap<>();

    public ChatHandler(PermissionService service) {
        this.service = service;
        service.onRankChange(this::invalidateCache);
    }

    public EventNode<Event> createNode() {
        EventNode<Event> node = EventNode.all("chat-handler");
        node.addListener(PlayerChatEvent.class, this::onChat);
        return node;
    }

    private void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUuid();
        String rawMessage = event.getRawMessage();

        PlayerData data = service.getPlayerByName(player.getUsername()).orElse(null);
        if (data == null) {
            data = service.getOrCreatePlayer(uuid, player.getUsername());
        }

        RankDisplay display = getDisplay(data.rankName());

        Component nameComponent = Component.text(player.getUsername(), display.nameColor);
        Component messageComponent = LEGACY.deserialize(rawMessage);

        Component formatted = Component.empty()
                .append(display.prefix)
                .append(Component.text(" "))
                .append(nameComponent)
                .append(Component.text(": "))
                .append(messageComponent);

        event.setFormattedMessage(formatted);
    }

    private RankDisplay getDisplay(String rankName) {
        RankDisplay cached = displayCache.get(rankName);
        if (cached != null) return cached;

        Rank rank = service.getRank(rankName).orElse(null);
        RankDisplay display;
        if (rank != null) {
            TextColor color = parseColor(rank.color());
            Component prefix = rank.prefix().isEmpty() ? EMPTY_PREFIX : LEGACY.deserialize(rank.prefix());
            display = new RankDisplay(prefix, color);
        } else {
            display = new RankDisplay(EMPTY_PREFIX, DEFAULT_COLOR);
        }

        displayCache.put(rankName, display);
        return display;
    }

    private void invalidateCache() {
        displayCache.clear();
    }

    private TextColor parseColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return DEFAULT_COLOR;
        int idx = -1;
        for (int i = 0; i < colorCode.length(); i++) {
            char c = colorCode.charAt(i);
            if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4'
                    || c == '5' || c == '6' || c == '7' || c == '8' || c == '9'
                    || c == 'a' || c == 'b' || c == 'c' || c == 'd' || c == 'e' || c == 'f'
                    || c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' || c == 'F') {
                idx = i;
                break;
            }
        }
        if (idx == -1) return DEFAULT_COLOR;
        char code = Character.toLowerCase(colorCode.charAt(idx));
        return switch (code) {
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
            case 'f' -> NamedTextColor.WHITE;
            default -> DEFAULT_COLOR;
        };
    }

    private record RankDisplay(Component prefix, TextColor nameColor) {}
}
