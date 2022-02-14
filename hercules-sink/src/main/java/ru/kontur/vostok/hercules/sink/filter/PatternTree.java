package ru.kontur.vostok.hercules.sink.filter;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.TypeUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>The class is a tree of patterns, the traversal of which allows you
 * to determine whether the tested sequence of {@link Variant}s fits any pattern.</p>
 *
 * <p>The tree supports the primitive {@link Variant} types (see {@link Type}).
 *
 * <p>The tree supports star {@code '*'} in the pattern definition. It means {@code any value}.</p>
 *
 * <p>The pattern consists of elements separated by {@code ':'}.
 * The number of elements in the pattern must strictly correspond to the number of {@link Type}s specified during initialization.
 * Each element in the string representation, except for {@code '*'}, must match the {@link Type} specified during initialization.</p>
 *
 * <p>Example:</p>
 * <p>let the tree be initialized with such list of types: [{@link Type#STRING}, {@link Type#INTEGER}, {@link Type#FLOAT}].
 * After that you can put in the tree patterns like {@code 'my_project:999:3.14'}, {@code 'my_project:1234:*'}, {@code '*:999:*'}, etc.
 * </p>
 *
 * @author Anton Akkuzin
 */
public class PatternTree {

    private final Node root;
    private final List<Type> types;
    private final int depth;

    public PatternTree(@NotNull List<Type> types) {
        for (Type type : types) {
            if (!type.isPrimitive()) {
                throw new IllegalArgumentException(String.format("Type '%s' is not primitive.", type));
            }
        }

        this.root = Node.newStar();
        this.types = types;
        this.depth = types.size();
    }

    /**
     * <p>Rebuilds the tree on each call. Patterns with common paths are merged together.</p>
     * <p>See {@link PatternTree} for pattern examples.</p>
     *
     * @param pattern   {@link String} with elements separated by {@code ':'}
     */
    public void put(@NotNull String pattern) {
        String[] tokens = pattern.split(":");
        if (tokens.length != depth) {
            throw new IllegalArgumentException("Pattern size should be equal to paths size.");
        }

        Node current = root;

        for (int i = 0; i < tokens.length; i++) {
            boolean star = tokens[i].equals("*");

            if (star && !current.containsStar()) {
                current.starChild = Node.newStar();
            }

            current = star
                    ? current.starChild
                    : current.children.computeIfAbsent(TypeUtil.parsePrimitiveValue(tokens[i], types.get(i)), Node::new);
        }
    }

    /**
     * <p>Iterates the tree and checks the given sequence of {@link Variant}s matches any of the patterns.</p>
     *
     * <p>Pattern branches that are specified explicitly (not an {@code '*'}) are checked first.</p>
     *
     * @param variants  sequence of {@link Variant}s to check
     * @return  {@code true} if {@code variants} matches any of the patterns
     */
    public boolean matches(List<Variant> variants) {
        return testNode(root, variants, 0);
    }

    /**
     * Returns {@code true} if this tree contains no branches.
     *
     * @return {@code true} if this tree contains no branches.
     */
    public boolean isEmpty() {
        return this.root.children.isEmpty() && !this.root.containsStar();
    }

    private boolean testNode(Node node, List<Variant> variants, int currentDepth) {
        if (currentDepth == this.depth) {
            return true;
        }

        Node child = node.getChild(variants.get(currentDepth), types.get(currentDepth));

        if (child != null && testNode(child, variants, currentDepth + 1)) {
            return true;
        }

        return node.containsStar() && testNode(node.starChild, variants, currentDepth + 1);
    }

    private static class Node {

        final Map<Object, Node> children = new HashMap<>();
        final Object value;
        Node starChild;

        private Node(Object value) {
            this.value = value;
        }

        private static Node newStar() {
            return new Node(null);
        }

        private Node getChild(Variant variant, Type type) {
            if (variant == null || variant.getType() != type) {
                return null;
            }

            for (Node child : children.values()) {
                if (child.matches(variant)) {
                    return child;
                }
            }

            return null;
        }

        private boolean matches(Variant variant) {
            return variant.getType() == Type.STRING
                    ? Arrays.equals(((TinyString) value).getBytes(), (byte[]) variant.getValue())
                    : Objects.equals(value, variant.getValue());
        }

        private boolean containsStar() {
            return starChild != null;
        }
    }
}
