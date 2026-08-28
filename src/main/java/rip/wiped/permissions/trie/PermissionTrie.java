package rip.wiped.permissions.trie;

import java.util.Set;

/**
 * Lock-free trie for O(m) permission lookups where m is the number of dot-separated segments.
 * Supports wildcard matching: "foo.*" matches "foo.bar", "foo.bar.baz" matches via parent.
 * Each node stores a lazy boolean for exact-match and a volatile reference for children.
 */
public final class PermissionTrie {

    private static final String WILDCARD = "*";

    private final Node root = new Node();

    public void insert(String permission) {
        if (permission == null || permission.isEmpty()) return;
        String[] segments = permission.split("\\.");
        Node current = root;
        for (String segment : segments) {
            current = current.getOrCreateChild(segment);
        }
        current.setExact(true);
    }

    public void remove(String permission) {
        if (permission == null || permission.isEmpty()) return;
        String[] segments = permission.split("\\.");
        removeRecursive(root, segments, 0);
    }

    private boolean removeRecursive(Node node, String[] segments, int depth) {
        if (depth == segments.length) {
            boolean wasExact = node.isExact();
            node.setExact(false);
            return wasExact && node.childCount() == 0;
        }
        Node child = node.getChild(segments[depth]);
        if (child == null) return false;
        boolean shouldRemove = removeRecursive(child, segments, depth + 1);
        if (shouldRemove) {
            node.removeChild(segments[depth]);
            return !node.isExact() && node.childCount() == 0;
        }
        return false;
    }

    /**
     * Check if the given permission is granted by this trie.
     * Checks exact match, prefix wildcards (parent "foo.*" grants "foo.bar"),
     * and trailing wildcards (granting "foo.*" grants "foo.bar").
     */
    public boolean has(String permission) {
        if (permission == null || permission.isEmpty()) return false;
        String[] segments = permission.split("\\.");
        return hasRecursive(root, segments, 0);
    }

    private boolean hasRecursive(Node node, String[] segments, int depth) {
        if (depth == segments.length) {
            return node.isExact();
        }
        String segment = segments[depth];

        // Exact child match
        Node child = node.getChild(segment);
        if (child != null && hasRecursive(child, segments, depth + 1)) {
            return true;
        }

        // Wildcard child: "*" at this depth grants all remaining
        Node wildcard = node.getChild(WILDCARD);
        if (wildcard != null && wildcard.isExact()) {
            return true;
        }

        return false;
    }

    public void clear() {
        root.removeAllChildren();
        root.setExact(false);
    }

    public int size() {
        return countRecursive(root);
    }

    private int countRecursive(Node node) {
        int count = node.isExact() ? 1 : 0;
        for (Node child : node.children().values()) {
            count += countRecursive(child);
        }
        return count;
    }

    /**
     * Internal trie node using a ConcurrentHashMap for thread-safe concurrent reads.
     */
    static final class Node {
        private final java.util.concurrent.ConcurrentHashMap<String, Node> children = new java.util.concurrent.ConcurrentHashMap<>(4);
        private volatile boolean exact;

        Node getOrCreateChild(String segment) {
            return children.computeIfAbsent(segment, k -> new Node());
        }

        Node getChild(String segment) {
            return children.get(segment);
        }

        void removeChild(String segment) {
            children.remove(segment);
        }

        void removeAllChildren() {
            children.clear();
        }

        int childCount() {
            return children.size();
        }

        boolean isExact() {
            return exact;
        }

        void setExact(boolean exact) {
            this.exact = exact;
        }

        java.util.concurrent.ConcurrentHashMap<String, Node> children() {
            return children;
        }
    }
}
