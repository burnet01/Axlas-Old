package rip.wiped.permissions.model;

/**
 * Persisted punishment state for a single username (lowercased).
 * A zero ban/mute expiry means the respective punishment is not active.
 */
public record PunishmentData(
        long banExpiresAt,
        String banReason,
        long muteExpiresAt,
        String muteReason,
        int warnings) {

    public static final PunishmentData EMPTY = new PunishmentData(0L, null, 0L, null, 0);
}