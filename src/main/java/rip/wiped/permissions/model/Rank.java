package rip.wiped.permissions.model;

import java.util.Collections;
import java.util.Set;

public final class Rank {

    private final String name;
    private final int weight;
    private final String prefix;
    private final String color;
    private final Set<String> permissions;

    public Rank(String name, int weight, String prefix, String color, Set<String> permissions) {
        this.name = name;
        this.weight = weight;
        this.prefix = prefix;
        this.color = color;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public String name() {
        return name;
    }

    public int weight() {
        return weight;
    }

    public String prefix() {
        return prefix;
    }

    public String color() {
        return color;
    }

    public Set<String> permissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Rank{name='" + name + "', weight=" + weight + "}";
    }
}
