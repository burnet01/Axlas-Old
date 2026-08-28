package rip.wiped.permissions.model;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final String username;
    private final String rankName;
    private final Set<String> extraPermissions;
    private final Set<String> deniedPermissions;

    public PlayerData(UUID uuid, String username, String rankName,
                      Set<String> extraPermissions, Set<String> deniedPermissions) {
        this.uuid = uuid;
        this.username = username;
        this.rankName = rankName;
        this.extraPermissions = Collections.unmodifiableSet(extraPermissions);
        this.deniedPermissions = Collections.unmodifiableSet(deniedPermissions);
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    public String rankName() {
        return rankName;
    }

    public Set<String> extraPermissions() {
        return extraPermissions;
    }

    public Set<String> deniedPermissions() {
        return deniedPermissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerData other)) return false;
        return uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
