package rip.wiped.permissions.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import rip.wiped.permissions.FakePlayerManager;

/**
 * Provides tab-completion entries for player name arguments.
 * Combines online players with registered fake players.
 */
public final class PlayerSuggestions {

    private PlayerSuggestions() {}

    public static void suggest(Suggestion suggestion) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            suggestion.addEntry(new SuggestionEntry(player.getUsername()));
        }
        for (String name : FakePlayerManager.getAll().keySet()) {
            suggestion.addEntry(new SuggestionEntry(name));
        }
    }
}