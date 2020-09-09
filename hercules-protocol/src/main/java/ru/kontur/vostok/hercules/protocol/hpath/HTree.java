package ru.kontur.vostok.hercules.protocol.hpath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.TinyString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Prefix tree (or Trie) is constructed from a set of {@link HPath}.
 * <p>
 * {@link HTree} can store values of type {@link T} in its nodes.
 * <p>
 * The main use case is
 * a partial traversal of {@link ru.kontur.vostok.hercules.protocol.Event} payload.
 *
 * @author Gregory Koshelev
 */
public class HTree<T> {
    private final HNode<T> root;

    public HTree() {
        root = new HNode<>(HPath.empty());
    }

    /**
     * Add {@code value} by {@code path} to the tree.
     *
     * @param path  the path
     * @param value the value
     * @return previously added {@code value} or {@code null} otherwise
     */
    @Nullable
    public T put(HPath path, T value) {
        HNode<T> current = root;
        HPath.TagIterator iterator = path.it();
        while (iterator.hasNext()) {
            TinyString next = iterator.next();
            HNode<T> parent = current;
            HPath base = parent.path;
            HNode<T> child = parent.children.computeIfAbsent(next, (tag) -> new HNode<T>(parent, HPath.combine(base, tag)));
            current = child;
        }
        T previousValue = current.value;
        current.value = value;
        return previousValue;
    }

    /**
     * {@link Navigator} navigates to parent or child nodes. Starts from the root of tree.
     *
     * @return navigator
     */
    public Navigator navigator() {
        return new Navigator();
    }

    /**
     * {@link Navigator} navigates to parent or child nodes. Starts from the root of tree.
     */
    public class Navigator {
        private HNode<T> current = HTree.this.root;

        /**
         * Returns the value is stored in the current node.
         *
         * @return the value is stored in the current node, or {@code null} otherwise
         */
        @Nullable
        public T getValue() {
            return current.value;
        }

        /**
         * Navigates to the child node.
         *
         * @param tag the child node tag
         * @return {@code true} if the child node exists, otherwise {@code false}
         */
        public boolean navigateToChild(TinyString tag) {
            HNode<T> child = current.children.get(tag);
            if (child == null) {
                return false;
            }
            current = child;
            return true;
        }

        /**
         * Navigates to the parent node.
         * <p>
         * Do nothing if the current is the root itself.
         */
        public void navigateToParent() {
            current = current.parent;
        }

        /**
         * Returns {@code true} if the current node has child nodes.
         *
         * @return {@code true} if the current node has child nodes, otherwise {@code false}
         */
        public boolean hasChildren() {
            return !current.children.isEmpty();
        }

        public boolean hasValue() {
            return current.value != null;
        }

        /**
         * Returns {@code true} if the current node is the root.
         *
         * @return {@code true} if the current node is the root, otherwise {@code false}
         */
        public boolean isRoot() {
            return current == HTree.this.root;
        }

        /**
         * Returns path to the current node.
         *
         * @return path of the current node
         */
        public HPath path() {
            return current.path;
        }

        /**
         * Return the set of child nodes tags.
         * <p>
         * This set is a view. It means that changes in the {@link HTree} are also applied to the set.
         *
         * @return the set of child nodes tags
         */
        public Set<TinyString> children() {
            return current.children.keySet();
        }
    }

    private static class HNode<T> {
        final Map<TinyString, HNode<T>> children = new LinkedHashMap<>();
        final HNode<T> parent;
        final HPath path;
        T value;

        private HNode(@NotNull HPath path) {
            this.parent = this;
            this.path = path;
        }

        private HNode(@NotNull HNode<T> parent, @NotNull HPath path) {
            this.path = path;
            this.parent = parent;
        }
    }
}
